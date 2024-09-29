package com.tanvir.features.liveroom.application.service;

import com.tanvir.core.util.enums.*;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.content.application.port.in.ContentUseCase;
import com.tanvir.features.gift.domain.valueobjects.ResourceFormat;
import com.tanvir.features.host.application.port.in.HostUseCase;
import com.tanvir.features.host.domain.Host;
import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.level.domain.Level;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.in.dto.request.*;
import com.tanvir.features.liveroom.application.port.in.dto.response.*;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import com.tanvir.features.liveroom.application.port.out.LiveRoomPersistencePort;
import com.tanvir.features.liveroom.domain.valueobject.*;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.metaproperty.application.port.in.MetaPropertyUseCase;
import com.tanvir.features.metaproperty.domain.MetaProperty;
import com.tanvir.features.user.application.port.in.UserUseCase;
import com.tanvir.features.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testng.util.Strings;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class LiveRoomService implements LiveRoomUseCase {
    private final UserUseCase userUseCase;
    private final LiveRoomPersistencePort port;
    private final MetaPropertyUseCase metaPropertyUseCase;
    private final ModelMapper modelMapper;
    private final TransactionalOperator rxtx;
    private final CachePort cachePort;
    private final HostUseCase hostUseCase;
    private final ContentUseCase contentUseCase;
    private final LevelUseCase levelUseCase;

    public LiveRoomService(UserUseCase userUseCase, LiveRoomPersistencePort port, MetaPropertyUseCase metaPropertyUseCase, ModelMapper modelMapper, TransactionalOperator rxtx, CachePort cachePort, HostUseCase hostUseCase, ContentUseCase contentUseCase, LevelUseCase levelUseCase) {
        this.userUseCase = userUseCase;
        this.port = port;
        this.metaPropertyUseCase = metaPropertyUseCase;
        this.modelMapper = modelMapper;
        this.rxtx = rxtx;
        this.cachePort = cachePort;
        this.hostUseCase = hostUseCase;
        this.contentUseCase = contentUseCase;
        this.levelUseCase = levelUseCase;
    }

    @Override
    public Mono<LiveRoomGridViewResponseDto> createStream(LiveRoomRequestDto requestDto) {
        AtomicReference<String> liveRoomId = new AtomicReference<>();
        return this.validateCreateStreamRequest(requestDto)
                .flatMap(liveRoomRequestDto -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId()))
                .doOnNext(user -> log.info("User received : {}", user))
                .doOnError(throwable -> log.error("Error happened while retrieving user : {}", throwable.getMessage()))
                .filter(user -> user.getUserType().equals(UserTypeEnum.USER_TYPE_HOST.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User must be a host to create LiveRoom.")))
                .flatMap(user ->
                        hostUseCase.getHostByUserId(user.getId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User must be a host to create LiveRoom.")))
                        .filter(host -> host.getActive().equals(Constants.STATUS_YES.getValue()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Host is Banned. Cannot create LiveRoom.")))
                        .flatMap(host -> port.getActiveLiveRoomByHostId(host.getId())
                        .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                        .switchIfEmpty(Mono.just(LiveRoom.builder().build()))
                        .flatMap(liveRoom ->
                            liveRoom.getId() != null
                                    ? Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User already has an active LiveRoom."))
                                    : Mono.just(liveRoom))
                        .thenReturn(host)))
                .flatMap(host -> Mono.just(buildLiveRoomDomain(host, requestDto))
                    .doOnNext(liveRoom -> log.info("LiveRoom domain built: {}", liveRoom))
                    .flatMap(port::saveLiveRoom)
                    .doOnNext(liveRoom -> liveRoomId.set(liveRoom.getId()))
                    .doOnSuccess(liveRoom -> log.info("LiveRoom saved into db"))
                    .doOnError(throwable -> log.error("Error happened while saving LiveRoom into db : {}", throwable.getMessage()))
                    .flatMap(liveRoom -> cachePort.create(this.buildFirebaseEntity(liveRoom, host))
                            .doOnNext(firebaseEntity -> log.info("LiveRoom saved into firebase successfully"))
                            .doOnError(throwable -> log.error("Error Happened while saving LiveRoom into Firebase : {}", throwable.getMessage()))
                            .thenReturn(liveRoom)))
                .map(liveRoom -> LiveRoomGridViewResponseDto
                        .builder()
                        .userMessage("Live room created successfully.")
                        .data(List.of(this.buildLiveRoomResponse(liveRoom)))
                        .count(1)
                        .build())
                .as(rxtx::transactional)
                .onErrorResume(throwable -> {
                    if (liveRoomId.get() != null) {
                        log.error("deleting firebase LiveRoom : {}", liveRoomId.get());
                        return Mono.defer(() -> cachePort.delete(liveRoomId.get()))
                                .doOnNext(s -> log.info("deleted : {}", s))
                                .doOnSuccess(userRepresentation -> log.info("LiveRoom deleted successfully from firebase with id: {}", liveRoomId.get()))
                                .then(Mono.error(throwable));
                    } else {
                        return Mono.error(throwable);
                    }
                });
    }

    private Mono<LiveRoomRequestDto> validateCreateStreamRequest(LiveRoomRequestDto requestDto) {
        List<String> validTypes = Arrays.asList(Constants.LIVE_ROOM_TYPE_AUDIO.getValue(), Constants.LIVE_ROOM_TYPE_VIDEO.getValue());

        if (!validTypes.contains(requestDto.getType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid LiveRoom Type!"));
        }

        return Mono.just(requestDto);
    }

    private LiveRoomFirebaseEntity buildFirebaseEntity(LiveRoom liveRoom, Host host) {
        HostSummary hostSummary = HostSummary
                .builder()
                .id(host.getId())
                .userId(host.getUserId())
                .displayName(host.getDisplayName())
                .gender(host.getGender())
                .profileImageId(host.getProfileImageId())
                .profileImageUrl(host.getProfileImageUrl())
                .userLevel(host.getUserLevel())
                .gemsCount(liveRoom.getHostDailyGems())
                .dailyStarProgress(DailyStarProgress.builder().build())
                .build();

        return LiveRoomFirebaseEntity
                .builder()
                .id(liveRoom.getId())
                .thumbnailId(liveRoom.getThumbnailId())
                .thumbnailUrl(liveRoom.getThumbnailUrl())
                .title(liveRoom.getTitle())
                .description(liveRoom.getDescription())
                .tags(liveRoom.getTags())
                .type(liveRoom.getType())
                .status(liveRoom.getStatus())
                .country(host.getCountry())
                .host(hostSummary)
                .viewers(new ArrayList<>())
                .viewerCount(0)
                .announcements(new ArrayList<>())
//                .elapsedSeconds(0)
                .build();
    }

    @Override
    public Mono<StreamResponseDto> joinStream(LiveRoomViewerRequestDto liveRoomViewerRequestDto) {
        return port.getLiveRoomById(liveRoomViewerRequestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + liveRoomViewerRequestDto.getLiveRoomId())))
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot join.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(liveRoomViewerRequestDto.getKeycloakId())
                        .flatMap(user -> {

                            if (liveRoom.getUserId().equals(user.getId())) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Host Cannot join his/her own LiveRoom."));
                            }

                            if (liveRoom.getViewerIds() != null && liveRoom.getViewerIds().contains(user.getId())) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User already joined the LiveRoom."));
                            }

                            Viewer viewer = Viewer
                                    .builder()
                                    .userId(user.getId())
                                    .displayName(user.getDisplayName())
                                    .gender(user.getGender())
                                    .profilePictureUrl(user.getProfileImageUrl())
                                    .frameUrl(user.getProfileFrameUrl())
                                    .userLevel(user.getUserLevel())
                                    .build();
                            liveRoom.setViewer(viewer);
                            liveRoom.setViewerCount(liveRoom.getViewerCount() + 1);

                            List<String> currentViewerIdsInLiveroom = new ArrayList<>(liveRoom.getViewerIds() != null && !liveRoom.getViewerIds().isEmpty()
                                    ? liveRoom.getViewerIds() : new ArrayList<>());
                            currentViewerIdsInLiveroom.add(viewer.getUserId());
                            liveRoom.setViewerIds(currentViewerIdsInLiveroom);

                            return Mono.just(liveRoom);
                        }))
                .flatMap(liveRoom -> {
                    if (liveRoom.getKickedOutUserIds() == null || liveRoom.getKickedOutUserIds().isEmpty()) {
                        liveRoom.setKickedOutUserIds(new ArrayList<>());
                    }
                    return Mono.just(liveRoom);
                })
                .filter(liveRoom -> !liveRoom.getKickedOutUserIds().contains(liveRoom.getViewer().getUserId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is kicked out from LiveRoom. Cannot join.")))
                .flatMap(liveRoom -> port.saveLiveRoom(liveRoom)
                        .thenReturn(liveRoom))
                .flatMap(this::buildJoinAnnouncement)
                .flatMap(liveRoom -> cachePort.update(liveRoom)
                        .doOnRequest(liveRoomEntity -> log.info("Requesting to update LiveRoom into firebase"))
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage())))
                .flatMap(liveRoom -> this.buildJoinStreamResponseDto(liveRoom, "User has successfully joined the room."))
                .doOnError(throwable -> log.error("Failed to Update LiveRoom with fan Entry. Error : {}", throwable.getMessage()));

    }

    private Mono<StreamResponseDto> buildJoinStreamResponseDto(LiveRoom liveRoom, String message) {
        RoomDataDto roomDataDto = new RoomDataDto();
        roomDataDto.setId(liveRoom.getId());
        roomDataDto.setJoinedOn(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        roomDataDto.setAnnouncement(liveRoom.getAnnouncement());
        return Mono.just(StreamResponseDto
                .builder()
                .message(message)
                .data(roomDataDto)
                .count(1)
                .build());
    }

    private Mono<StreamResponseDto> buildLeaveStreamResponseDto(LiveRoom liveRoom, String message) {
        RoomDataDto roomDataDto = new RoomDataDto();
        roomDataDto.setId(liveRoom.getId());
        roomDataDto.setLeftOn(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        return Mono.just(StreamResponseDto
                .builder()
                .message(message)
                .data(roomDataDto)
                .count(1)
                .build());
    }

    private Mono<StreamResponseDto> buildCommentStreamResponseDto(LiveRoom liveRoom, String message) {
        RoomDataDto roomDataDto = new RoomDataDto();
        roomDataDto.setId(liveRoom.getId());
        roomDataDto.setCommentedOn(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        return Mono.just(StreamResponseDto
                .builder()
                .message(message)
                .data(roomDataDto)
                .count(1)
                .build());
    }

    private Mono<StreamResponseDto> buildKickViewerStreamResponseDto(LiveRoom liveRoom, String message) {
        RoomDataDto roomDataDto = new RoomDataDto();
        roomDataDto.setId(liveRoom.getId());
        roomDataDto.setKickedOn(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        return Mono.just(StreamResponseDto
                .builder()
                .message(message)
                .data(roomDataDto)
                .count(1)
                .build());
    }

    private Mono<LiveRoom> buildJoinAnnouncement(LiveRoom liveRoom) {
//        todo : build announcement
        return userUseCase.getUserById(liveRoom.getViewer().getUserId())
                .flatMap(user -> {
                    Announcement announcement = new Announcement();
                    if (Strings.isNotNullAndNotEmpty(user.getRideId())) {
                        return contentUseCase.getContentById(user.getRideId())
                                .map(content -> {
                                    announcement.setMessageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_RIDE.getValue()));
                                    announcement.setType(AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_RIDE.getValue());
                                    announcement.setTime(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
                                    List<String> imageUrlList = content.getResourceFormats()
                                            .stream()
                                            .filter(resourceFormat -> resourceFormat.getResourceType().equals("IMAGE"))
                                            .map(ResourceFormat::getThumbnailUrl).toList();
                                    announcement.setResource(
                                            Announcement.Resource
                                                .builder()
                                                    .name(content.getName())
                                                    .imageUrl(!imageUrlList.isEmpty() ? imageUrlList.get(0) : null)
                                                .build()
                                    );
                                    return Tuples.of(announcement, user);
                                });
                    } else if (Strings.isNotNullAndNotEmpty(user.getEntryCardId())) {
                        return contentUseCase.getContentById(user.getEntryCardId())
                                .map(content -> {
                                    announcement.setMessageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_ENTRY_CARD.getValue()));
                                    announcement.setType(AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_ENTRY_CARD.getValue());
                                    announcement.setTime(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
                                    List<String> imageUrlList = content.getResourceFormats()
                                            .stream()
                                            .filter(resourceFormat -> resourceFormat.getResourceType().equals("IMAGE"))
                                            .map(ResourceFormat::getThumbnailUrl).toList();
                                    announcement.setResource(
                                            Announcement.Resource
                                                    .builder()
                                                    .name(content.getName())
                                                    .imageUrl(!imageUrlList.isEmpty() ? imageUrlList.get(0) : null)
                                                    .build()
                                    );
                                    return Tuples.of(announcement, user);
                                });
                    } else if (Strings.isNullOrEmpty(user.getEntryCardId()) && Strings.isNullOrEmpty(user.getRideId())) {
                        announcement.setMessageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_CASUAL.getValue()));
                        announcement.setType(AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_CASUAL.getValue());
                        announcement.setTime(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
                        return Mono.just(Tuples.of(announcement, user));
                    }
                    return Mono.just(Tuples.of(announcement, user));
                })
                .flatMap(announcementAndUserTuple -> {
                    Announcement announcement = announcementAndUserTuple.getT1();
                    User user = announcementAndUserTuple.getT2();
                    return levelUseCase.getLevelDomainByLevel(liveRoom.getViewer().getUserLevel())
                            .map(level -> {
                                announcement.setMentionedUser(AnnouncementUser
                                        .builder()
                                        .userId(user.getId())
                                        .name(user.getDisplayName())
                                        .levelUrl(level.getLevelBadgeUrl())
                                        .build());
                                return announcement;
                            });
                })
                .map(announcement -> {
                    liveRoom.setAnnouncement(announcement);
                    return liveRoom;
                });
    }

    private Mono<LiveRoom> buildKickOutAnnouncement(LiveRoom liveRoom, User host, User viewer) {
        Announcement announcement = new Announcement();
        announcement.setType(AnnouncementEnum.ANNOUNCEMENT_TYPE_KICK.getValue());
        announcement.setTime(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
        announcement.setMessageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_KICK.getValue()));


        return levelUseCase
                .getLevelDomainByLevel(host.getUserLevel())
                .map(Level::getLevelBadgeUrl)
                .zipWith(levelUseCase.getLevelDomainByLevel(viewer.getUserLevel())
                        .map(Level::getLevelBadgeUrl))
                .map(hostAndViewerLevelUrl -> {
                    announcement.setPublisher(AnnouncementUser
                            .builder()
                            .userId(host.getId())
                            .name(host.getDisplayName())
                            .levelUrl(hostAndViewerLevelUrl.getT1())
                            .build());

                    announcement.setMentionedUser(AnnouncementUser
                            .builder()
                            .userId(viewer.getId())
                            .name(viewer.getDisplayName())
                            .levelUrl(hostAndViewerLevelUrl.getT2())
                            .build());

                    return announcement;
                })
                .map(announcement1 -> {
                    liveRoom.setAnnouncement(announcement1);
                    return liveRoom;
                });
    }

    @Override
    public Mono<StreamResponseDto> leaveStream(LiveRoomViewerRequestDto requestDto) {
        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + requestDto.getLiveRoomId())))
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot leave.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .flatMap(user -> this.updateLiveRoomForFanLeave(liveRoom, user)))
                .doOnNext(liveRoom -> log.info("Updated LiveRoom with fan leave : {}", liveRoom))
                .flatMap(liveRoom1 -> port.saveLiveRoom(liveRoom1)
                        .thenReturn(liveRoom1))
                .flatMap(liveRoom -> cachePort.updateForViewerLeave(liveRoom)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage())))
                .flatMap(liveRoom -> this.buildLeaveStreamResponseDto(liveRoom, "User successfully left the live room."))
                .doOnError(throwable -> log.error("Failed to Update LiveRoom with fan Leave. Error : {}", throwable.getMessage()));
    }

    @Override
    public Mono<StreamResponseDto> endStream(String liveRoomId, String keycloakId) {
        return port.getLiveRoomById(liveRoomId)
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + liveRoomId)))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot End.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(keycloakId)
                        .filter(user -> liveRoom.getUserId().equals(user.getId()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not the host of the LiveRoom. Cannot end.")))
                        .map(user -> liveRoom))
                .map(liveRoom -> {
                    liveRoom.setStatus(Constants.STATUS_OFFLINE.getValue());
                    liveRoom.setEndedOn(LocalDateTime.now());
                    liveRoom.setDurationInSeconds((liveRoom.getEndedOn().toEpochSecond(ZoneOffset.UTC) - liveRoom.getCreatedOn().toEpochSecond(ZoneOffset.UTC)));
                    return liveRoom;
                })
                .flatMap(port::saveLiveRoom)
                .flatMap(liveRoom ->
                        cachePort.delete(liveRoomId)
                        .doOnSuccess(liveRoomEntity -> log.info("LiveRoom deleted from firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while deleting LiveRoom from Firebase : {}", throwable.getMessage()))
                        .thenReturn(liveRoom))
                .flatMap(this::buildEndStreamResponseDto);
    }

    @Override
    public Mono<StreamResponseDto> kickOutUser(KickOutUserRequestDto requestDto) {
        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + requestDto.getLiveRoomId())))
                .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot kick out.")))
                .filter(liveRoom -> liveRoom.getViewerIds().contains(requestDto.getUserId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Viewer doesn't exist in LiveRoom. Cannot kick out.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                            .filter(host -> liveRoom.getUserId().equals(host.getId()))
                            .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not the host of the LiveRoom. Cannot kick out.")))
                            .flatMap(host -> userUseCase.getUserById(requestDto.getUserId())
                            .flatMap(kickedUser -> this.updateLiveRoomForKick(liveRoom, kickedUser)
                                    .thenReturn(kickedUser))
                            .flatMap(kickedUser -> this.buildKickOutAnnouncement(liveRoom, host, kickedUser))))
                .flatMap(liveRoom1 -> port.saveLiveRoom(liveRoom1)
                        .thenReturn(liveRoom1))
                .flatMap(liveRoom -> cachePort.updateForViewerKick(liveRoom)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .thenReturn(liveRoom))
                .flatMap(liveRoom -> this.buildKickViewerStreamResponseDto(liveRoom, "User successfully kicked out from LiveRoom."))
                .as(rxtx::transactional);
    }

    private Mono<LiveRoom> updateLiveRoomForKick(LiveRoom liveRoom, User kickedUser) {
        if (liveRoom.getKickedOutUserIds() == null || liveRoom.getKickedOutUserIds().isEmpty()) {
            liveRoom.setKickedOutUserIds(List.of(kickedUser.getId()));
        } else {
            List<String> updatedKickedOutUsers = new ArrayList<>(liveRoom.getKickedOutUserIds());
            updatedKickedOutUsers.add(kickedUser.getId());
            liveRoom.setKickedOutUserIds(updatedKickedOutUsers);
        }
        liveRoom.setViewerCount(liveRoom.getViewerCount() - 1);

        Viewer viewer = Viewer
                .builder()
                .userId(kickedUser.getId())
                .displayName(kickedUser.getDisplayName())
                .gender(kickedUser.getGender())
                .profilePictureUrl(kickedUser.getProfileImageUrl())
                .frameUrl(kickedUser.getProfileFrameUrl())
                .userLevel(kickedUser.getUserLevel())
                .build();
        liveRoom.setViewer(viewer);
        return Mono.just(liveRoom);
    }

    @Override
    public Mono<LiveRoomResponseDto> getLiveRoomDetailViewById(String id) {
        return port.getLiveRoomById(id)
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + id)))
                .map(this::buildLiveRoomResponse)
                .map(liveRoomResponse -> this.buildLiveRoomResponseDto(liveRoomResponse, "LiveRoom Detail Fetched Successfully."));
    }


    @Override
    public Mono<LiveRoomGridViewResponseDto> getHomepage(GridViewRequestDto requestDto) {

        return this.validateGridViewRequest(requestDto)
                .flatMap(requestDto1 -> requestDto.getViewMode().equals(Constants.TAB_PARTY.getValue())
                    ? port.getActiveLiveRoomsCountByTypeAndCountry(Constants.LIVE_ROOM_TYPE_AUDIO.getValue(), requestDto.getCountry(), requestDto.getViewMode())
                        .zipWith(port.getActiveAudioLiveRooms(requestDto.getPageable(), requestDto.getCountry()).collectList())
                        .map(countAndDataTuple -> LiveRoomGridViewResponseDto
                                .builder()
                                .userMessage("LiveRoom Grid View Fetched Successfully.")
                                .data(this.buildLiveRoomResponse(countAndDataTuple.getT2()))
                                .count(countAndDataTuple.getT1().intValue())
                                .build())
                    : port.getActiveLiveRoomsCountByTypeAndCountry(Constants.LIVE_ROOM_TYPE_VIDEO.getValue(), requestDto.getCountry(), requestDto.getViewMode())
                        .zipWith(port.getActiveVideoLiveRooms(requestDto.getPageable(), requestDto.getCountry()).collectList())
                        .map(countAndDataTuple -> LiveRoomGridViewResponseDto
                                .builder()
                                .userMessage("LiveRoom Grid View Fetched Successfully.")
                                .data(this.buildLiveRoomResponse(countAndDataTuple.getT2()))
                                .count(countAndDataTuple.getT1().intValue())
                                .build()));
    }

    private Mono<LiveRoomGridViewResponseDto> getVideoLiveRooms(GridViewRequestDto requestDto) {
//        todo : implement fetch according to popular index

        return null;
    }

    private Mono<GridViewRequestDto> validateGridViewRequest(GridViewRequestDto requestDto) {
        List<String> validViewModes = Arrays.asList(Constants.TAB_PARTY.getValue(), Constants.TAB_POPULAR.getValue(), Constants.TAB_FRESHER.getValue());
        if (!validViewModes.contains(requestDto.getViewMode())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid viewMode!"));
        }

        return Mono.just(requestDto);

    }

    @Override
    public Mono<LiveRoomResponseDto> sendGift(SendGiftRequestDto requestDto) {
        return rxtx.transactional(
                    port.getLiveRoomById(requestDto.getLiveRoomId())
                    .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, ExceptionMessages.NO_LIVE_ROOM_FOUND_WITH_ID.getValue().concat(requestDto.getLiveRoomId()))))
                    /*.filter(liveRoom -> liveRoom.getFans().containsKey(requestDto.getFanUserId()))
                    .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, ExceptionMessages.USER_DOESNT_EXIST_IN_ROOM.getValue())))*/
                    .flatMap(liveRoom -> userUseCase.getUserById(requestDto.getFanUserId())
                            .filter(fanUser -> fanUser.getBeans() > requestDto.getGiftBeans())
                            .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, ExceptionMessages.INSUFFICIENT_BEANS.getValue())))
                            .flatMap(fanUser -> Mono.zip(Mono.just(liveRoom), Mono.just(fanUser), metaPropertyUseCase.getMetaPropertyByDescription(MetaPropertyEnums.INDEX_META_PROPERTY.getValue()))))
                    .flatMap(tuple3 -> this.updateFanAndHostBeansCountGemsCountLevelPercentage(tuple3, requestDto))
                    .map(liveRoomResponse -> this.buildLiveRoomResponseDto(liveRoomResponse, "Gift Sent Successfully")));
    }

    @Override
    public Mono<StreamResponseDto> comment(LiveRoomViewerRequestDto requestDto) {
        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + requestDto.getLiveRoomId())))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User not found!")))
                        .filter(user -> liveRoom.getViewerIds().contains(user.getId()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not a viewer of the LiveRoom. Cannot comment.")))
                        .flatMap(user -> levelUseCase.getLevelDomainByLevel(user.getUserLevel())
                            .map(level -> {
                                Announcement announcement = Announcement
                                    .builder()
                                    .type(AnnouncementEnum.ANNOUNCEMENT_TYPE_COMMENT.getValue())
                                    .time(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString())
                                    .messageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_COMMENT.getValue()) + requestDto.getComment())
                                    .publisher(AnnouncementUser
                                            .builder()
                                            .userId(user.getId())
                                            .name(user.getDisplayName())
                                            .levelUrl(level.getLevelBadgeUrl())
                                            .build())
                                    .build();

                                liveRoom.setAnnouncement(announcement);
                                return liveRoom;
                            })))
                .flatMap(liveRoom1 -> cachePort.updateForComment(liveRoom1)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .thenReturn(liveRoom1))
                .flatMap(liveRoom1 -> this.buildCommentStreamResponseDto(liveRoom1, "Comment posted successfully."))
                .as(rxtx::transactional);

    }

    @Override
    public Mono<LiveRoom> getLiveRoomById(String id) {
        return port.getLiveRoomById(id);
    }

    private Mono<LiveRoomResponse> updateFanAndHostBeansCountGemsCountLevelPercentage(Tuple3<LiveRoom, User, MetaProperty> tuple, SendGiftRequestDto requestDto) {
        LiveRoom liveRoom = tuple.getT1();
        User fanUser = tuple.getT2();
        MetaProperty metaProperty = tuple.getT3();

        fanUser.setBeans(fanUser.getBeans() - requestDto.getGiftBeans());
       /* liveRoom.setBeansCount(liveRoom.getBeansCount() == null
                                    ? requestDto.getGiftBeans()
                                    : liveRoom.getBeansCount() + requestDto.getGiftBeans());
        liveRoom.setStarCount((int) (liveRoom.getBeansCount() / metaProperty.getStarIndex()));
        int remainingBeans = (int) (liveRoom.getBeansCount() % (liveRoom.getStarCount() * metaProperty.getStarIndex()));
        liveRoom.setLevelCompletionPercentage((remainingBeans * 100) / metaProperty.getStarIndex());*/


        return rxtx.transactional(
                userUseCase.saveUser(fanUser)
                .flatMap(user -> port.saveLiveRoom(liveRoom))
                /*.flatMap(updatedLiveRoom -> userUseCase.getUserById(updatedLiveRoom.getUserId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, ExceptionMessages.USER_NOT_FOUND.getValue())))
                        .flatMap(host -> {
                            host.setGems(host.getGems() == 0
                                    ? requestDto.getGiftBeans()
                                    : host.getGems() + requestDto.getGiftBeans());
                            *//*host.setGemsCount(host.getGemsCount() == null
                                    ? (long)(requestDto.getGiftBeans() * (metaProperty.getGemsConversionRate() / 100))
                                    : host.getGemsCount() + (long)(requestDto.getGiftBeans() * (metaProperty.getGemsConversionRate() / 100)));*//*
                            return userUseCase.saveUser(host)
                                    .thenReturn(updatedLiveRoom);
                        }))*/
                .map(updatedLiveRoom -> modelMapper.map(updatedLiveRoom, LiveRoomResponse.class)));
    }


    private Mono<StreamResponseDto> buildEndStreamResponseDto(LiveRoom liveRoom) {
        return Mono.just(StreamResponseDto
                .builder()
                .message("LiveStream Ended Successfully")
                .data(RoomDataDto
                        .builder()
                        .id(liveRoom.getId())
                        .endedOn(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                        .build())
                .build());
    }

    private EndStreamResponseDto buildUserInfo(User user, EndStreamResponseDto responseDto) {
        User userInfo = User
                .builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .country(user.getCountry())
                .profileImageId(user.getProfileImageId())
                .profileImageUrl(user.getProfileImageUrl())
                .gems(user.getGems())
                .userLevel(user.getUserLevel())
                .build();

        responseDto.setUserInfo(userInfo);
        return responseDto;
    }


    private EndStreamResponseDto buildLiveStreamInfo(LiveRoom liveRoom, EndStreamResponseDto responseDto) {
        DecimalFormat decimalFormat = new DecimalFormat("00");
        LiveStreamInfo liveStreamInfo = modelMapper.map(liveRoom, LiveStreamInfo.class);
//        liveStreamInfo.setDurationInSeconds(liveRoom.getDuration());

//        long hours = liveRoom.getDuration() / 3600;
//        long minutes = (liveRoom.getDuration() % 3600) / 60;
//        long seconds = (liveRoom.getDuration() - (hours * 3600) - (minutes * 60));
//        liveStreamInfo.setDurationString(decimalFormat.format(hours) + ":" + decimalFormat.format(minutes) + ":" + decimalFormat.format(seconds));

        responseDto.setLiveStreamInfo(liveStreamInfo);
        responseDto.setUserMessage("LiveStream Ended Successfully");
        return responseDto;
    }


    private Mono<LiveRoomGridViewResponseDto> getGridViewResponseForEmptyList() {
        return Mono.just(LiveRoomGridViewResponseDto
                .builder()
                .userMessage("LiveRoom Grid View Fetched Successfully.")
                .data(new ArrayList<>())
                .count(0)
                .build());
    }


    /*private Mono<LiveRoomGridViewResponseDto> getGridViewByTab(List<LiveRoom> liveRoomList, GridViewRequestDto requestDto) {
        return this.filterLiveRoomsAccordingToTypeAndTag(liveRoomList, requestDto)
            .map(filteredLiveRoomList -> filteredLiveRoomList.stream().sorted(Comparator.comparing(LiveRoom::getStarCount)).toList())
            .flatMap(sortedLiveRoomList -> this.getPaginatedLiveRoomList(sortedLiveRoomList, requestDto)
                .map(paginatedLiveRoomList -> LiveRoomGridViewResponseDto
                        .builder()
                        .userMessage("LiveRoom Grid View Fetched Successfully.")
                        .data(this.buildLiveRoomResponse(paginatedLiveRoomList))
                        .count(sortedLiveRoomList.size())
                        .build())
            );
    }*/

    /*private Mono<List<LiveRoom>> filterLiveRoomsAccordingToTypeAndTag(List<LiveRoom> liveRoomList, GridViewRequestDto requestDto) {

        if (requestDto.getViewMode().equalsIgnoreCase(Constants.TAB_PARTY.getValue())) {
            List<LiveRoom> audioLiveRoomList = liveRoomList.stream()
                    .filter(liveRoom -> liveRoom.getType().equalsIgnoreCase(Constants.LIVE_ROOM_TYPE_AUDIO.getValue()))
                    .toList();
            return  Mono.just(audioLiveRoomList);
        }

        return metaPropertyUseCase
                .getMetaPropertyByDescription(MetaPropertyEnums.INDEX_META_PROPERTY.getValue())
                .map(MetaProperty::getPopularIndex)
                .map(popularIndex -> {
                    List<LiveRoom> popularList = liveRoomList.stream().filter(liveRoom -> liveRoom.getPopularityLevel() >= popularIndex).toList();
                    List<LiveRoom> freshersList = liveRoomList.stream().filter(liveRoom -> liveRoom.getPopularityLevel() <= popularIndex).toList();
                    return requestDto.getViewMode().equalsIgnoreCase(Constants.TAB_POPULAR.getValue())
                            ? popularList
                            : freshersList;
                });
    }*/

    private Mono<List<LiveRoom>> getPaginatedLiveRoomList(List<LiveRoom> liveRoomList, GridViewRequestDto requestDto) {
        /*int offset = requestDto.getOffset() == null || requestDto.getOffset() == 0
                        ? 0 : requestDto.getOffset();
        int limit = requestDto.getLimit() == null || requestDto.getLimit() == 0
                        ? 20 : requestDto.getLimit();*/
        int offset = requestDto.getPageable().getPageNumber();
        int limit = requestDto.getPageable().getPageSize();

        List<LiveRoom> paginatedLiveRoomList = liveRoomList.stream()
                .skip((long) offset * limit)
                .limit(limit)
                .toList();

        return Mono.just(paginatedLiveRoomList);

    }


    /*private LiveRoomResponse sortAndLimitFansByEntryTimeAndCount(LiveRoomResponse liveRoomResponse, Long fanCount) {
        List<Fan> fanList = liveRoomResponse.getFans()
                .stream()
                .sorted(Comparator.comparing(Fan::getEntryTime))
                .limit(fanCount)
                .toList();

        liveRoomResponse.setFans(fanList);
        return liveRoomResponse;
    }*/


    private LiveRoomResponse buildLiveRoomResponse(LiveRoom liveRoom) {
        LiveRoomResponse liveRoomResponse = modelMapper.map(liveRoom, LiveRoomResponse.class);
        liveRoomResponse.setAnnouncement(liveRoom.getAnnouncement());
        liveRoomResponse.setCreatedOn(liveRoom.getCreatedOn().toInstant(ZoneOffset.UTC));
        return liveRoomResponse;
    }


    private List<LiveRoomResponse> buildLiveRoomResponse(List<LiveRoom> liveRoomList) {
        List<LiveRoomResponse> liveRoomResponseList = new ArrayList<>();
        for (LiveRoom liveRoom : liveRoomList) {
//            Map<String, Fan> fanMap = liveRoom.getFans();
            LiveRoomResponse liveRoomResponse = modelMapper.map(liveRoom, LiveRoomResponse.class);
            liveRoomResponse.setCreatedOn(liveRoom.getCreatedOn().toInstant(ZoneOffset.UTC));
//            liveRoomResponse.setFans(fanMap.values().stream().toList());
            liveRoomResponseList.add(liveRoomResponse);
        }

        return liveRoomResponseList;
    }


    private LiveRoom updateLiveRoomForFanEntry(LiveRoom liveRoom, LiveRoomViewerRequestDto requestDto) {
//        Map<String, Fan> fans = liveRoom.getFans();
//        Fan newFan = requestDto.getFan();
//        newFan.setEntryTime(LocalDateTime.now());

       /* fans.put(newFan.getUserId(), newFan);
        liveRoom.setFans(fans);
        liveRoom.setFansCount(fans.size());*/
        return liveRoom;
    }


    private Mono<LiveRoom> updateLiveRoomForFanLeave(LiveRoom liveRoom, User user) {
       if (liveRoom.getViewerIds() != null && !liveRoom.getViewerIds().contains(user.getId())) {
           return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User doesn't exist in Stream! Can't Leave."));
       }


       if (liveRoom.getViewerIds() != null) {
           liveRoom.getViewerIds().remove(user.getId());
           liveRoom.setViewerCount(liveRoom.getViewerIds().size());
       }


        Viewer viewer = Viewer
                .builder()
                .userId(user.getId())
                .displayName(user.getDisplayName())
                .gender(user.getGender())
                .profilePictureUrl(user.getProfileImageUrl())
                .frameUrl(user.getProfileFrameUrl())
                .userLevel(user.getUserLevel())
                .build();
        liveRoom.setViewer(viewer);

        return Mono.just(liveRoom);
    }


    private LiveRoomResponseDto buildLiveRoomResponseDto(LiveRoomResponse liveRoomResponse, String userMessage) {

        return LiveRoomResponseDto
                .builder()
                .userMessage(userMessage)
                .data(liveRoomResponse)
                .build();
    }


    private LiveRoom buildLiveRoomDomain(Host host, LiveRoomRequestDto requestDto) {
        return LiveRoom
                .builder()
                .id(UUID.randomUUID().toString())
                .thumbnailId(requestDto.getThumbnailId())
                .thumbnailUrl(requestDto.getThumbnailUrl())
                .title(Strings.isNotNullAndNotEmpty(requestDto.getTitle())
                        ? requestDto.getTitle()
                        : host.getDisplayName())
                .description(requestDto.getDescription())
                .tags(requestDto.getTags())
                .type(requestDto.getType())
                .status(Constants.STATUS_LIVE.getValue())
                .country(host.getCountry())
                .hostId(host.getId())
                .userId(host.getUserId())
                .kickedOutUserIds(new ArrayList<>())
                .viewerCount(0)
                .hostDailyGems(0)
                .createdOn(LocalDateTime.now())
//                .userId(user.getId())
//                .keycloakId(requestDto.getKeycloakId())
//                .starCount(user.get)
//                .gemsCount(user.getGems())
//                .popularityLevel(user.getPopularityLevel())
//                .userLevel(user.getUserLevel())
                /*.profilePicture(Strings.isNullOrEmpty(requestDto.getProfilePicture())
                        ? user.getProfileImageId()
                        : requestDto.getProfilePicture())*/
                /*.welcomeNote(Strings.isNullOrEmpty(requestDto.getWelcomeNote())
                        ? null
                        : requestDto.getWelcomeNote())*/
//                .fans(new HashMap<>())
//                .createdBy(requestDto.getUserId())
                .build();
    }
}
