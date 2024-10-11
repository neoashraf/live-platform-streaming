package com.tanvir.features.liveroom.application.service;

import com.tanvir.core.util.enums.*;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.agora.service.AgoraService;
import com.tanvir.features.agora.service.AgoraTokenRequestDto;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.content.application.port.in.ContentUseCase;
import com.tanvir.features.level.domain.valueobjects.ResourceFormat;
import com.tanvir.features.host.application.port.in.HostUseCase;
import com.tanvir.features.host.domain.Host;
import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.in.dto.request.*;
import com.tanvir.features.liveroom.application.port.in.dto.response.*;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import com.tanvir.features.liveroom.application.port.out.LiveRoomPersistencePort;
import com.tanvir.features.liveroom.domain.valueobject.*;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroomactivity.LiveRoomActivityService;
import com.tanvir.features.liveroomsummary.application.port.in.LiveRoomSummaryUseCase;
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
import reactor.core.scheduler.Schedulers;
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
    private final AgoraService agoraService;
    private final LiveRoomActivityService liveRoomActivityService;
    private final CommonBusiness commonBusiness;
    private final LiveRoomSummaryUseCase liveRoomSummaryUseCase;

    public LiveRoomService(UserUseCase userUseCase, LiveRoomPersistencePort port, MetaPropertyUseCase metaPropertyUseCase, ModelMapper modelMapper, TransactionalOperator rxtx, CachePort cachePort, HostUseCase hostUseCase, ContentUseCase contentUseCase, LevelUseCase levelUseCase, AgoraService agoraService, LiveRoomActivityService liveRoomActivityService, CommonBusiness commonBusiness, LiveRoomSummaryUseCase liveRoomSummaryUseCase) {
        this.userUseCase = userUseCase;
        this.port = port;
        this.metaPropertyUseCase = metaPropertyUseCase;
        this.modelMapper = modelMapper;
        this.rxtx = rxtx;
        this.cachePort = cachePort;
        this.hostUseCase = hostUseCase;
        this.contentUseCase = contentUseCase;
        this.levelUseCase = levelUseCase;
        this.agoraService = agoraService;
        this.liveRoomActivityService = liveRoomActivityService;
        this.commonBusiness = commonBusiness;
        this.liveRoomSummaryUseCase = liveRoomSummaryUseCase;
    }

    @Override
    public Mono<StreamResponseDto> createStream(LiveRoomRequestDto requestDto) {
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
                .flatMap(host -> this.buildLiveRoomDomain(host, requestDto)
                    .doOnNext(liveRoom -> log.info("LiveRoom domain built: {}", liveRoom))
                    .flatMap(port::saveLiveRoom)
                    .map(liveRoom -> {
                        liveRoomId.set(liveRoom.getId());
                        return liveRoom;
                    })
                    .doOnSuccess(liveRoom -> log.info("LiveRoom saved into db"))
                    .doOnError(throwable -> log.error("Error happened while saving LiveRoom into db : {}", throwable.getMessage()))
                    .doOnNext(liveRoom -> this.buildFirebaseEntity(liveRoom, host)
                            .flatMap(cachePort::create)
                            .doOnNext(firebaseEntity -> log.info("LiveRoom saved into firebase successfully"))
                            .doOnError(throwable -> log.error("Error Happened while saving LiveRoom into Firebase : {}", throwable.getMessage()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe()))
                /*.map(liveRoom -> LiveRoomGridViewResponseDto
                        .builder()
                        .userMessage("Live room created successfully.")
                        .data(List.of(this.buildLiveRoomResponse(liveRoom)))
                        .count(1)
                        .build())*/
                .flatMap(liveRoom -> this.buildCreateStreamResponseDto(requestDto, liveRoom, "Live room created successfully."))
                .doOnError(throwable -> log.error("Failed to Create stream response dto. Error : {}", throwable.getMessage()))
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

    private Mono<LiveRoomFirebaseEntity> buildFirebaseEntity(LiveRoom liveRoom, Host host) {

        return liveRoomActivityService.getDailyReceivedGems(host.getUserId())
                .flatMap(currentGems -> levelUseCase.getLevelDomainByLevel(host.getUserLevel())
                        .map(level -> {
                            ResourceFormat levelResource = CommonBusiness.getResourceFormatByResourceType(level.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                            host.setLevelBadgeUrl(levelResource.getResourceUrl());
                            return host;
                        })
                        .doOnError(throwable -> log.error("Error happened while setting level url: {}", throwable.getMessage()))
                        .map(host1 -> {
                            DailyStarProgress starProgress = this.calculateStarProgress(currentGems);
                            HostSummary hostSummary = HostSummary
                                    .builder()
                                    .userId(host.getUserId())
                                    .hostMaxId(host.getMaxId())
                                    .displayName(host.getDisplayName())
                                    .gender(host.getGender())
                                    .profileImageUrl(host.getProfileImageUrl())
                                    .userLevel(host.getUserLevel())
                                    .levelBadgeUrl(host.getLevelBadgeUrl())
                                    .gemsCount(liveRoom.getHostDailyGems())
                                    .dailyStarProgress(starProgress)
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
                                    .build();
                        }));

    }

    private DailyStarProgress calculateStarProgress(double currentGems) {
        int currentStar = 0;
        double nextStarGems = 0;
        double gemsNeededForNextStar = 0;

        if (currentGems >= 2000000) {
            currentStar = 5;
            nextStarGems = 2000000;
            gemsNeededForNextStar = 0;
        } else if (currentGems >= 1000000) {
            currentStar = 4;
            nextStarGems = 2000000;
            gemsNeededForNextStar = 2000000 - currentGems;
        } else if (currentGems >= 200000) {
            currentStar = 3;
            nextStarGems = 1000000;
            gemsNeededForNextStar = 1000000 - currentGems;
        } else if (currentGems >= 50000) {
            currentStar = 2;
            nextStarGems = 200000;
            gemsNeededForNextStar = 200000 - currentGems;
        } else if (currentGems >= 10000) {
            currentStar = 1;
            nextStarGems = 50000;
            gemsNeededForNextStar = 50000 - currentGems;
        } else {
            currentStar = 0;
            nextStarGems = 10000;
            gemsNeededForNextStar = 10000 - currentGems;
        }

        return DailyStarProgress
                .builder()
                .starLevel(currentStar)
                .nextStarLevel(Math.min(currentStar + 1, 5))
                .dailyReceivedGemsValue(currentGems)
                .dailyReceivedGemsName(CommonBusiness.convertToShortName(currentGems))
                .nextLevelGemsValue(nextStarGems)
                .nextLevelGemsName(CommonBusiness.convertToShortName(nextStarGems))
                .trailingByNextLevelGemsValue(gemsNeededForNextStar)
                .trailingByNextLevelGemsName(CommonBusiness.convertToShortName(gemsNeededForNextStar))
                .build();
    }

    @Override
    public Mono<StreamResponseDto> joinStream(LiveRoomViewerRequestDto liveRoomViewerRequestDto) {
        List<String> validTokenTypes = List.of(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_UID_AND_PRIVILEGE.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_RTM.getValue());
        if (!validTokenTypes.contains(liveRoomViewerRequestDto.getTokenType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid token type"));
        }

        return port.getLiveRoomById(liveRoomViewerRequestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + liveRoomViewerRequestDto.getLiveRoomId())))
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot join.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(liveRoomViewerRequestDto.getKeycloakId())
                        .flatMap(commonBusiness::setUserLevelUrl)
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
                                    .maxId(user.getMaxId())
                                    .displayName(user.getDisplayName())
                                    .gender(user.getGender())
                                    .profilePictureUrl(user.getProfileImageUrl())
                                    .frameUrl(user.getProfileFrameUrl())
                                    .userLevel(user.getUserLevel())
                                    .levelBadgeUrl(user.getLevelBadgeUrl())
                                    .build();
                            liveRoom.setViewer(viewer);
                            liveRoom.setViewerCount(liveRoom.getViewerCount() + 1);

                            List<String> currentViewerIdsInLiveroom = new ArrayList<>(liveRoom.getViewerIds() != null && !liveRoom.getViewerIds().isEmpty()
                                    ? liveRoom.getViewerIds() : new ArrayList<>());
                            currentViewerIdsInLiveroom.add(viewer.getUserId());
                            liveRoom.setViewerIds(currentViewerIdsInLiveroom);
                            liveRoom.setTotalViewerCount(liveRoom.getTotalViewerCount() + 1);

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
                .flatMap(this::buildJoinAnnouncement)
                .doOnNext(liveRoom -> port.saveLiveRoom(liveRoom)
                        .flatMap(liveRoom1 -> cachePort.update(liveRoom))
                        .doOnRequest(liveRoomEntity -> log.info("Requesting to update LiveRoom into firebase"))
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .flatMap(liveRoom -> this.buildJoinStreamResponseDto(liveRoomViewerRequestDto, liveRoom, "User has successfully joined the room."))
                .doOnError(throwable -> log.error("Failed to Update LiveRoom with fan Entry. Error : {}", throwable.getMessage()));

    }

    private Mono<StreamResponseDto> buildJoinStreamResponseDto(LiveRoomViewerRequestDto requestDto, LiveRoom liveRoom, String message) {
        RoomDataDto roomDataDto = new RoomDataDto();
        roomDataDto.setId(liveRoom.getId());
        roomDataDto.setHostMaxId(liveRoom.getHostMaxId());
        roomDataDto.setJoinedOn(LocalDateTime.now().toInstant(ZoneOffset.UTC));
//        roomDataDto.setAnnouncement(liveRoom.getAnnouncement());
        roomDataDto.setWelcomeAnnouncement(liveRoom.getWelcomeAnnouncement());
        roomDataDto.setViewerMaxId(liveRoom.getViewer().getMaxId());
        AgoraTokenRequestDto agoraTokenRequestDto =
                AgoraTokenRequestDto
                        .builder()
                        .channelName(liveRoom.getId())
                        .role(AgoraTokenTypeEnum.ROLE_SUBSCRIBER.getValue())
                        .uid(Integer.parseInt(liveRoom.getViewer().getMaxId()))
                        .tokenExpirationInSeconds(86400)
                        .tokenType(requestDto.getTokenType())
                        .build();

        return agoraService.generateToken(agoraTokenRequestDto)
                .doOnError(throwable -> log.error("Error happened while generating Agora Token : {}", throwable.getMessage()))
                .map(agoraTokenResponseDto -> {
                    if (agoraTokenResponseDto.getData() != null && !agoraTokenResponseDto.getData().isEmpty()) {
                       if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithUid());
                       } else if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithUserAccount());
                       } else if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_UID_AND_PRIVILEGE.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithUidAndPrivilege());
                       } else if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithAccountAndPrivilege());
                       } else if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_RTM.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithRtm());
                       }
                    }
                    return roomDataDto;
                })
                .onErrorMap(throwable -> new ExceptionHandlerUtil(HttpStatus.INTERNAL_SERVER_ERROR, "Error happened while generating Agora Token!"))
                .map(agoraTokenResponseDto -> StreamResponseDto
                        .builder()
                        .message(message)
                        .data(roomDataDto)
                        .count(1)
                        .build());

    }

    private Mono<StreamResponseDto> buildCreateStreamResponseDto(LiveRoomRequestDto requestDto, LiveRoom liveRoom, String message) {
        RoomDataDto roomDataDto = new RoomDataDto();
        roomDataDto.setId(liveRoom.getId());
        roomDataDto.setHostMaxId(liveRoom.getHostMaxId());
        roomDataDto.setUserId(liveRoom.getUserId());

        roomDataDto.setThumbnailId(liveRoom.getThumbnailId());
        roomDataDto.setThumbnailUrl(liveRoom.getThumbnailUrl());
        roomDataDto.setTitle(liveRoom.getTitle());
        roomDataDto.setType(liveRoom.getType());
        roomDataDto.setTags(liveRoom.getTags());
        roomDataDto.setStatus(liveRoom.getStatus());
        roomDataDto.setViewerCount(liveRoom.getViewerCount());
        roomDataDto.setCreatedOn(liveRoom.getCreatedOn().toInstant(ZoneOffset.UTC));
        roomDataDto.setCountry(liveRoom.getCountry());
        roomDataDto.setViewer(liveRoom.getViewer());
        roomDataDto.setHostDailyGems(liveRoom.getHostDailyGems());

        requestDto.setTokenType(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue());
        AgoraTokenRequestDto agoraTokenRequestDto =
                AgoraTokenRequestDto
                        .builder()
                        .channelName(liveRoom.getId())
                        .role(AgoraTokenTypeEnum.ROLE_PUBLISHER.getValue())
                        .uid(Integer.parseInt(liveRoom.getHostMaxId()))
                        .tokenExpirationInSeconds(86400)
                        .tokenType(requestDto.getTokenType())
                        .build();

        return agoraService.generateToken(agoraTokenRequestDto)
                .doOnError(throwable -> log.error("Error happened while generating Agora Token : {}", throwable.getMessage()))
                .map(agoraTokenResponseDto -> {
                    if (agoraTokenResponseDto.getData() != null && !agoraTokenResponseDto.getData().isEmpty()) {
                       if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithUid());
                       } else if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithUserAccount());
                       } else if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_UID_AND_PRIVILEGE.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithUidAndPrivilege());
                       } else if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithAccountAndPrivilege());
                       } else if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_RTM.getValue())) {
                           roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getTokenWithRtm());
                       }
                    }
                    return roomDataDto;
                })
                .onErrorMap(throwable -> new ExceptionHandlerUtil(HttpStatus.INTERNAL_SERVER_ERROR, "Error happened while generating Agora Token!"))
                .map(agoraTokenResponseDto -> StreamResponseDto
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

    private Mono<LiveRoomJoinPermissionResponseDto> buildEnableJoinResponseDTO(LiveRoom liveRoom, String message) {
        EnableJoinPermission joinPermission = new EnableJoinPermission();
        joinPermission.setJoinCallAvailable(liveRoom.getEnableJoin());
        joinPermission.setRoomId(liveRoom.getId());
        return Mono.just(LiveRoomJoinPermissionResponseDto
                .builder()
                .message(message)
                .data(joinPermission)
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
                                    announcement.setAnnouncementId(UUID.randomUUID().toString());
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
                                    announcement.setAnnouncementId(UUID.randomUUID().toString());
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
                        announcement.setAnnouncementId(UUID.randomUUID().toString());
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
                                ResourceFormat imageResource = CommonBusiness.getResourceFormatByResourceType(level.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                                announcement.setMentionedUser(AnnouncementUser
                                        .builder()
                                        .userId(user.getId())
                                        .maxId(user.getMaxId())
                                        .name(user.getDisplayName())
                                        .levelUrl(imageResource.getResourceUrl())
                                        .build());

                                return announcement;
                            });
                })
                .flatMap(announcement -> {
                    return userUseCase.getUserById(liveRoom.getUserId())
                        .flatMap(host -> levelUseCase
                                .getLevelDomainByLevel(host.getUserLevel())
                                .map(level -> {
                                    ResourceFormat imageResource = CommonBusiness.getResourceFormatByResourceType(level.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                                    String levelUrl = imageResource.getResourceUrl();

                                    Announcement welcomeAnnouncement = new Announcement();
                                    welcomeAnnouncement.setPublisher(AnnouncementUser
                                            .builder()
                                            .userId(host.getId())
                                            .maxId(host.getMaxId())
                                            .name(host.getDisplayName())
                                            .levelUrl(levelUrl)
                                            .build());
                                    welcomeAnnouncement.setMentionedUser(announcement.getMentionedUser());
                                    welcomeAnnouncement.setMessageTemplate(AnnouncementEnum.ANNOUNCEMENT_MESSAGE_WELCOME.getValue().concat(liveRoom.getDescription()));
                                    log.info("Welcome Announcement message template: {}", welcomeAnnouncement.getMessageTemplate());
                                    liveRoom.setWelcomeAnnouncement(welcomeAnnouncement);
                                    return announcement;
                                }));
                })
                .map(announcement -> {
                    liveRoom.setAnnouncement(announcement);
                    return liveRoom;
                });
    }

    private Mono<LiveRoom> buildKickOutAnnouncement(LiveRoom liveRoom, User host, User viewer) {
        Announcement announcement = new Announcement();
        announcement.setAnnouncementId(UUID.randomUUID().toString());
        announcement.setType(AnnouncementEnum.ANNOUNCEMENT_TYPE_KICK.getValue());
        announcement.setTime(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
        announcement.setMessageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_KICK.getValue()));


        return levelUseCase
                .getLevelDomainByLevel(host.getUserLevel())
                .map(level -> {
                    ResourceFormat imageResource = CommonBusiness.getResourceFormatByResourceType(level.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                    return imageResource.getResourceUrl();
                })
                .zipWith(levelUseCase.getLevelDomainByLevel(viewer.getUserLevel())
                        .map(level -> {
                            ResourceFormat imageResource = CommonBusiness.getResourceFormatByResourceType(level.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                            return imageResource.getResourceUrl();
                        }))
                .map(hostAndViewerLevelUrl -> {
                    announcement.setPublisher(AnnouncementUser
                            .builder()
                            .userId(host.getId())
                            .maxId(host.getMaxId())
                            .name(host.getDisplayName())
                            .levelUrl(hostAndViewerLevelUrl.getT1())
                            .build());

                    announcement.setMentionedUser(AnnouncementUser
                            .builder()
                            .userId(viewer.getId())
                            .maxId(viewer.getMaxId())
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
                /*.filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot leave.")))*/
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .flatMap(user -> this.updateLiveRoomForFanLeave(liveRoom, user)))
                .doOnNext(liveRoom -> log.info("Updated LiveRoom with fan leave : {}", liveRoom))
                .flatMap(liveRoom1 -> port.saveLiveRoom(liveRoom1)
                        .thenReturn(liveRoom1))
                .doOnNext(liveRoom -> cachePort.updateForViewerLeave(liveRoom)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic()).subscribe())
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
                .doOnNext(liveRoom -> {
                    liveRoomSummaryUseCase.processLiveRoomSummary(liveRoom)
                            .doOnNext(liveRoomSummary -> log.info("LiveRoom Summary processed successfully"))
                            .doOnError(throwable -> log.error("Error Happened while processing LiveRoom Summary : {}", throwable.getMessage()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                })
                .doOnNext(liveRoom ->
                        cachePort.delete(liveRoomId)
                        .doOnSuccess(liveRoomEntity -> log.info("LiveRoom deleted from firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while deleting LiveRoom from Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
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

        if (liveRoom.getViewerIds() != null) {
            liveRoom.getViewerIds().remove(kickedUser.getId());
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
                .levelBadgeUrl(kickedUser.getLevelBadgeUrl())
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
                    : /*port.getActiveLiveRoomsCountByTypeAndCountry(Constants.LIVE_ROOM_TYPE_VIDEO.getValue(), requestDto.getCountry(), requestDto.getViewMode())
                        .zipWith(port.getActiveVideoLiveRooms(requestDto.getPageable(), requestDto.getCountry()).collectList())*/
                        port.getActiveLiveRoomsCountByTypeAndCountry(null, requestDto.getCountry(), requestDto.getViewMode())
                        .zipWith(port.getActiveVideoAndAudioLiveRooms(requestDto.getPageable(), requestDto.getCountry()).collectList())
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
                .map(liveRoom -> {
                    List<String> viewerIdList = liveRoom.getViewerIds() != null && !liveRoom.getViewerIds().isEmpty()
                            ? liveRoom.getViewerIds()
                            : new ArrayList<>();

                    liveRoom.setViewerIds(viewerIdList);
                    return liveRoom;
                })
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User not found!")))
                        .filter(user -> liveRoom.getViewerIds().contains(user.getId()) || liveRoom.getUserId().equals(user.getId()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not a viewer of the LiveRoom. Cannot comment.")))
                        .flatMap(user -> levelUseCase.getLevelDomainByLevel(user.getUserLevel())
                            .map(level -> {
                                ResourceFormat imageResource = CommonBusiness.getResourceFormatByResourceType(level.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                                Announcement announcement = Announcement
                                    .builder()
                                    .announcementId(UUID.randomUUID().toString())
                                    .type(AnnouncementEnum.ANNOUNCEMENT_TYPE_COMMENT.getValue())
                                    .time(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString())
                                    .messageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_COMMENT.getValue()) + requestDto.getComment())
                                    .publisher(AnnouncementUser
                                            .builder()
                                            .userId(user.getId())
                                            .name(user.getDisplayName())
//                                            .levelUrl(level.getLevelBadgeUrl())
                                            .levelUrl(imageResource.getResourceUrl())
                                            .build())
                                    .build();

                                liveRoom.setAnnouncement(announcement);
                                return liveRoom;
                            })))
                .doOnNext(liveRoom1 -> cachePort.updateForComment(liveRoom1)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .flatMap(liveRoom1 -> this.buildCommentStreamResponseDto(liveRoom1, "Comment posted successfully."))
                .as(rxtx::transactional);

    }

    @Override
    public Mono<LiveRoom> getLiveRoomById(String id) {
        return port.getLiveRoomById(id);
    }

    @Override
    public Mono<LiveRoom> updateLiveRoom(LiveRoom liveRoom) {
        return port.saveLiveRoom(liveRoom);
    }

    @Override
    public Mono<LiveRoomJoinPermissionResponseDto> setJoinPermission(JoinPermissionRequestDTO requestDTO) {
        return port.getLiveRoomById(requestDTO.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,"LiveRoom does not exist by the given id")))
                .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,"LiveRoom is not live")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDTO.getKeycloakId())
                            .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                            .map(User::getId)
                            .filter(userId -> liveRoom.getUserId().equals(userId))
                            .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,"User must be the host of the LiveRoom to set the permission")))
                        .thenReturn(liveRoom))
                .flatMap(liveRoom -> {
                    List<String> validTypes = Arrays.asList(Constants.STATUS_YES.getValue(), Constants.STATUS_NO.getValue());

                    if (!validTypes.contains(requestDTO.getEnableJoin())) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid EnableJoin Type!"));
                    }
                    liveRoom.setEnableJoin(requestDTO.getEnableJoin());
                    return port.saveLiveRoom(liveRoom);
                })
                .doOnNext(liveRoom1 -> cachePort.updateForJoinPermission(liveRoom1)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .flatMap(liveRoom -> this.buildEnableJoinResponseDTO(liveRoom,"Join call setting updated successfully."))
                .doOnError(throwable -> log.error("Error Happened while setting join permission: {}", throwable.getMessage()));
    }

    @Override
    public Mono<JoinCallResponseDto> requestJoinCall(JoinCallRequestDto requestDto) {
          return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,"LiveRoom does not found by the given id")))
                .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .filter(liveRoom -> liveRoom.getEnableJoin().equals(Constants.STATUS_YES.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,"LiveRoom is not live")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                        .filter(user -> user.getActive().equalsIgnoreCase(Constants.STATUS_YES.getValue()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,"User is not active")))
                        .map(user -> Tuples.of(liveRoom, user)))
                .flatMap(tupleOfLiveRoomAndUser ->
                  {
                      log.info("live room {} and userInfo : {}", tupleOfLiveRoomAndUser.getT1(), tupleOfLiveRoomAndUser.getT2());
                      return buildJoinRequest(tupleOfLiveRoomAndUser.getT2(), requestDto)
                              .flatMap(joinRequests -> {
                                  LiveRoom liveRoom = tupleOfLiveRoomAndUser.getT1();
                                  liveRoom.setJoinRequests(joinRequests);
                                  return port.saveLiveRoom(liveRoom).zipWith(Mono.just(tupleOfLiveRoomAndUser.getT2()));

                              });
                  })
                .doOnNext(liveRoomUserTuple2 -> cachePort.updateForJoinRequest(liveRoomUserTuple2.getT1())
                    .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                    .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe())
               .flatMap(roomUserTuple2 -> buildJoinRequestResponse(roomUserTuple2.getT1(), roomUserTuple2.getT2(), requestDto))
               .map(liveRoomJoinRequestInfo -> JoinCallResponseDto
                       .builder()
                       .message("Join call request placed successfully.")
                       .data(liveRoomJoinRequestInfo)
                       .count(1)
                       .error(false)
                       .build());
    }

    @Override
    public Mono<JoinCallResponseDto> processJoinCall(JoinCallRequestDto requestDto) {
        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found by the given id")))
                .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .filter(liveRoom -> liveRoom.getEnableJoin().equals(Constants.STATUS_YES.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not live")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                        .filter(user -> user.getActive().equalsIgnoreCase(Constants.STATUS_YES.getValue()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not active")))
                        .thenReturn(liveRoom))
                .flatMap(liveRoom -> {
                    List<String> validTypes = Arrays.asList(Constants.STATUS_PERMIT.getValue(), Constants.STATUS_DECLINE.getValue());

                    if (!validTypes.contains(requestDto.getAction())) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid Action Type!"));
                    }

                    Optional<JoinRequests> joinRequestOpt = liveRoom.getJoinRequests().stream()
                            .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                            .findFirst();

                    return joinRequestOpt.map(joinRequests -> {
                        joinRequests.setStatus(requestDto.getAction());
                        joinRequests.setReason(requestDto.getReason());
                        return port.saveLiveRoom(liveRoom);
                    }).orElseGet(() -> Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User request not found")));
                })
                .doOnNext(liveRoom -> cachePort.updateForProcessingJoinCall(liveRoom, requestDto)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error happened while updating LiveRoom into Firebase: {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .map(liveRoom -> buildJoinCallProcessData(liveRoom, requestDto))
                .map(liveRoomJoinRequestInfo -> JoinCallResponseDto.builder()
                        .message("Join call request placed successfully.")
                        .data(liveRoomJoinRequestInfo)
                        .count(1)
                        .error(false)
                        .build());
    }

    private LiveRoomJoinRequestInfo buildJoinCallProcessData(LiveRoom liveRoom, JoinCallRequestDto requestDto) {
        return LiveRoomJoinRequestInfo.builder()
                .roomId(requestDto.getLiveRoomId())
                .status(requestDto.getAction())
                .requestId(requestDto.getRequestId())
                .reason(requestDto.getReason())
                .build();
    }

    private Mono<LiveRoomJoinRequestInfo> buildJoinRequestResponse(LiveRoom liveRoom, User user, JoinCallRequestDto requestDto) {
        return Mono.create(sink -> {
            liveRoom.getJoinRequests().stream()
                    .filter(joinRequests -> joinRequests.getUserId().equals(user.getId()))
                    .findFirst()
                    .ifPresentOrElse(
                            joinRequest -> {
                                LiveRoomJoinRequestInfo responseDto = LiveRoomJoinRequestInfo.builder()
                                        .roomId(requestDto.getLiveRoomId())
                                        .status(liveRoom.getStatus())
                                        .requestId(joinRequest.getRequestId())
                                        .micOn(requestDto.getMicOn())
                                        .cameraOn(requestDto.getCameraOn())
                                        .cameraView(requestDto.getCameraView())
                                        .build();
                                sink.success(responseDto);
                            },
                            () -> sink.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Join request not found"))
                    );
        });
    }

    private Mono<List<JoinRequests>> buildJoinRequest(User user, JoinCallRequestDto requestDto) {
        return levelUseCase.getLevelDomainByLevel(user.getUserLevel())
                .map(level -> {
                    ResourceFormat imageResource = CommonBusiness.getResourceFormatByResourceType(level.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                    JoinRequests joinRequest = JoinRequests
                            .builder()
                            .profileLevelUrl(imageResource.getResourceUrl())
                            .requestId(UUID.randomUUID().toString())
                            .maxId(user.getMaxId())
                            .userId(user.getId())
                            .cameraOn(requestDto.getCameraOn())
                            .cameraView(requestDto.getCameraView())
                            .micOn(requestDto.getMicOn())
                            .displayName(user.getDisplayName())
                            .profileImageUrl(user.getProfileImageUrl())
                            .status(Constants.STATUS_PENDING.getValue())
                            .build();
                    return List.of(joinRequest);
                });
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
                        .durationInSeconds(liveRoom.getDurationInSeconds())
                        .duration(CommonBusiness.formatTimeToString(liveRoom.getDurationInSeconds()))
                        .totalViewerCount(liveRoom.getTotalViewerCount())
                        .build())
                .count(1)
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
        liveRoomResponse.setHostMaxId(liveRoom.getHostMaxId());
        liveRoomResponse.setAnnouncement(liveRoom.getAnnouncement());
        liveRoomResponse.setCreatedOn(liveRoom.getCreatedOn().toInstant(ZoneOffset.UTC));

        return liveRoomResponse;
    }

    private LiveRoomResponse buildLiveRoomCreateResponse(LiveRoom liveRoom) {
        LiveRoomResponse liveRoomResponse = modelMapper.map(liveRoom, LiveRoomResponse.class);
        liveRoomResponse.setHostMaxId(liveRoom.getHostMaxId());
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


    private Mono<LiveRoom> buildLiveRoomDomain(Host host, LiveRoomRequestDto requestDto) {
        return liveRoomActivityService.getDailyReceivedGems(host.getUserId())
                    .map(dailyReceivedGems -> LiveRoom
                        .builder()
                        .id(UUID.randomUUID().toString())
                        .thumbnailId(Strings.isNotNullAndNotEmpty(requestDto.getThumbnailId()) ? requestDto.getThumbnailId() : host.getProfileImageId())
                        .thumbnailUrl(Strings.isNotNullAndNotEmpty(requestDto.getThumbnailUrl()) ? requestDto.getThumbnailUrl() : host.getProfileImageUrl())
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
                        .hostMaxId(host.getMaxId())
                        .kickedOutUserIds(new ArrayList<>())
                        .viewerCount(0)
                        .hostDailyGems(dailyReceivedGems)
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
                        .build());
    }
}
