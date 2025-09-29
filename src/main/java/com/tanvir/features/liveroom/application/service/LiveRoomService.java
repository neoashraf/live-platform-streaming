package com.tanvir.features.liveroom.application.service;

import com.tanvir.core.util.FormatUtil;
import com.tanvir.core.util.enums.*;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.agora.service.AgoraService;
import com.tanvir.features.agora.service.AgoraTokenRequestDto;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.content.application.port.in.ContentUseCase;
import com.tanvir.features.content.domain.valueobjects.ContentTypeEnum;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePort;
import com.tanvir.features.gifttransaction.application.service.GiftTransactionService;
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
import com.tanvir.features.liveroom.domain.*;
import com.tanvir.features.liveroom.domain.valueobject.*;
import com.tanvir.features.liveroomactivity.LiveRoomActivityService;
import com.tanvir.features.liveroomsummary.adapter.out.persistence.entity.LiveRoomSummaryEntity;
import com.tanvir.features.liveroomsummary.application.port.in.LiveRoomSummaryUseCase;
import com.tanvir.features.liveroomsummary.application.service.LiveRoomSummaryService;
import com.tanvir.features.metaproperty.application.port.in.MetaPropertyUseCase;
import com.tanvir.features.metaproperty.domain.MetaProperty;
import com.tanvir.features.user.application.port.in.UserUseCase;
import com.tanvir.features.user.domain.User;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testng.util.Strings;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    private final LiveRoomSummaryService liveRoomSummaryService;
    private final GiftTransactionPersistencePort giftTransactionPersistencePort;

    public LiveRoomService(UserUseCase userUseCase, LiveRoomPersistencePort port, MetaPropertyUseCase metaPropertyUseCase, ModelMapper modelMapper, TransactionalOperator rxtx, CachePort cachePort, HostUseCase hostUseCase, ContentUseCase contentUseCase, LevelUseCase levelUseCase, AgoraService agoraService, LiveRoomActivityService liveRoomActivityService, CommonBusiness commonBusiness, LiveRoomSummaryUseCase liveRoomSummaryUseCase, LiveRoomSummaryService liveRoomSummaryService, GiftTransactionPersistencePort giftTransactionPersistencePort) {
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
        this.liveRoomSummaryService = liveRoomSummaryService;
        this.giftTransactionPersistencePort = giftTransactionPersistencePort;
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
                                        .thenReturn(Tuples.of(user, host))))
                .flatMap(userHostTuple2 -> this.buildLiveRoomDomain(userHostTuple2.getT2(), requestDto)
                        .doOnNext(liveRoom -> log.info("LiveRoom domain built: {}", liveRoom))
                        .flatMap(port::saveLiveRoom)
                        .map(liveRoom -> {
                            liveRoomId.set(liveRoom.getId());
                            return liveRoom;
                        })
                        .doOnSuccess(liveRoom -> log.info("LiveRoom saved into db"))
                        .doOnError(throwable -> log.error("Error happened while saving LiveRoom into db : {}", throwable.getMessage()))
                        .flatMap(liveRoom -> this.buildFirebaseEntity(liveRoom, userHostTuple2.getT2(), userHostTuple2.getT1())
                                .flatMap(cachePort::create)
                                .doOnNext(firebaseEntity -> log.info("LiveRoom saved into firebase successfully"))
                                .doOnError(throwable -> log.error("Error Happened while saving LiveRoom into Firebase : {}", throwable.getMessage()))
                                .thenReturn(liveRoom)))
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


    private Mono<LiveRoomFirebaseEntity> buildFirebaseEntity(LiveRoom liveRoom, Host host, User user) {
//        Integer audioSeatNumber = liveRoom.getAudioSeatNumber();
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
                                    .gems(host.getGems())
                                    .gemsValue(CommonBusiness.convertToShortName(host.getGems()))
                                    .dailyStarProgress(starProgress)
                                    .profileFrameId(user.getProfileFrameId())
                                    .profileFrameUrl(user.getProfileFrameUrl())
                                    .micOn(Constants.STATUS_YES.getValue())
                                    .build();
                            Map<String, SeatNumberDto> seatAvailableStatusMap = new HashMap<>();
                            SeatNumberDto seatDto = SeatNumberDto
                                    .builder()
                                    .availableStatus(true)
                                    .userId(null)
                                    .joinReqId(null)
                                    .build();

                            int videoLiveMaxSeat = 5;
                            int seatNumber = 0;

                            if (liveRoom.getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())) {
                                seatNumber = liveRoom.getAudioSeatNumber();
                            } else if (liveRoom.getType().equals(Constants.LIVE_ROOM_TYPE_VIDEO.getValue())) {
                                seatNumber = videoLiveMaxSeat;
                            }
                            for (int i = 0; i < seatNumber; i++) {
                                seatAvailableStatusMap.put("Seat_" + i, seatDto);
                            }

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
                                    .maxAudioParticipants(liveRoom.getMaxAudioParticipants())
                                    .audioParticipants(liveRoom.getAudioParticipants())
                                    .audioSkinId(Strings.isNotNullAndNotEmpty(liveRoom.getAudioSkinId()) ? liveRoom.getAudioSkinId() : "")
                                    .audioSkinUrl(Strings.isNotNullAndNotEmpty(liveRoom.getAudioSkinUrl()) ? liveRoom.getAudioSkinUrl() : "")
                                    .enableJoin(liveRoom.getEnableJoin())
                                    .enableAutoJoin(liveRoom.getEnableAutoJoin())
//                                    .seatAvailableStatus(seatAvailableStatus)
                                    .seatMap(seatAvailableStatusMap)
                                    .summary(Summary.builder().build())
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
        List<String> validTokenTypes = List.of(
                AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue(),
                AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT.getValue(),
                AgoraTokenTypeEnum.TOKEN_WITH_UID_AND_PRIVILEGE.getValue(),
                AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE.getValue(),
                AgoraTokenTypeEnum.TOKEN_WITH_RTM.getValue()
        );

        if (!validTokenTypes.contains(liveRoomViewerRequestDto.getTokenType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid token type"));
        }

        return port.getLiveRoomById(liveRoomViewerRequestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + liveRoomViewerRequestDto.getLiveRoomId())))
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Sorry! Host is offline.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(liveRoomViewerRequestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User not found!")))
                        .flatMap(commonBusiness::setUserLevelUrl)
                        .flatMap(user -> {
                            if (OfficialIdEnum.INVISIBLE_IDS.getValue().contains(user.getMaxId())) {
                                log.info("Invisible user (maxId: {}) joined silently.", user.getMaxId());
                                return buildJoinStreamResponseDto(liveRoomViewerRequestDto, liveRoom, user,
                                        "Invisible user joined silently.");
                            }

                            return joinUserIntoLiveRoom(liveRoom, user, liveRoomViewerRequestDto)
                                    .doOnNext(streamResponseDto -> log.info("User {} joined LiveRoom: {}", user.getMaxId(), liveRoom.getId()))
                                    .doOnError(error -> log.error("Error while joining LiveRoom: {}", error.getMessage()));
                        }))
                .doOnError(error -> log.error("Failed to update LiveRoom with viewer entry: {}", error.getMessage()));
    }

    private Mono<StreamResponseDto> joinUserIntoLiveRoom(LiveRoom liveRoom, User user, LiveRoomViewerRequestDto liveRoomViewerRequestDto) {
        if (liveRoom.getUserId().equals(user.getId())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,
                    "Host Cannot join his/her own LiveRoom."));
        }

        if (liveRoom.getViewerIds() != null && liveRoom.getViewerIds().contains(user.getId())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,
                    "User already joined the LiveRoom."));
        }

        Viewer viewer = Viewer.builder()
                .userId(user.getId())
                .maxId(user.getMaxId())
                .displayName(user.getDisplayName())
                .gender(user.getGender())
                .profilePictureUrl(user.getProfileImageUrl())
                .profileFrameUrl(user.getProfileFrameUrl())
                .profileFrameId(user.getProfileFrameId())
                .userLevel(user.getUserLevel())
                .levelBadgeUrl(user.getLevelBadgeUrl())
                .build();

        liveRoom.setViewer(viewer);
        liveRoom.setViewerCount(liveRoom.getViewerCount() + 1);

        List<String> viewerIds = new ArrayList<>(
                liveRoom.getViewerIds() != null ? liveRoom.getViewerIds() : new ArrayList<>()
        );
        viewerIds.add(viewer.getUserId());
        liveRoom.setViewerIds(viewerIds);
        liveRoom.setTotalViewerCount(liveRoom.getTotalViewerCount() + 1);

        if (liveRoom.getKickedOutUserIds() == null) {
            liveRoom.setKickedOutUserIds(new ArrayList<>());
        }

        if (liveRoom.getKickedOutUserIds().contains(viewer.getUserId())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,
                    "Sorry! Can't join the live stream. You have been kicked out by host."));
        }

        return buildJoinAnnouncement(liveRoom)
                .doOnNext(builtLiveRoom -> port.saveLiveRoom(builtLiveRoom)
                        .flatMap(liveRoom1 -> cachePort.update(builtLiveRoom))
                        .doOnRequest(req -> log.info("Requesting to update LiveRoom into Firebase"))
                        .doOnNext(resp -> log.info("LiveRoom updated into Firebase successfully"))
                        .doOnError(err -> log.error("Error while updating LiveRoom in Firebase: {}", err.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
                )
                .flatMap(updatedRoom ->
                        buildJoinStreamResponseDto(liveRoomViewerRequestDto, updatedRoom, user,
                                "User has successfully joined the room.")
                );
    }


    private Mono<StreamResponseDto> buildJoinStreamResponseDto(LiveRoomViewerRequestDto requestDto, LiveRoom liveRoom, User user, String message) {
        RoomDataDto roomDataDto = new RoomDataDto();
        roomDataDto.setId(liveRoom.getId());
        roomDataDto.setHostMaxId(liveRoom.getHostMaxId());
        roomDataDto.setJoinedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
//        roomDataDto.setAnnouncement(liveRoom.getAnnouncement());
        roomDataDto.setWelcomeAnnouncement(liveRoom.getWelcomeAnnouncement());
        roomDataDto.setViewerMaxId(user.getMaxId());
        AgoraTokenRequestDto agoraTokenRequestDto =
                AgoraTokenRequestDto
                        .builder()
                        .channelName(liveRoom.getId())
                        .role(AgoraTokenTypeEnum.ROLE_SUBSCRIBER.getValue())
                        .uid(Integer.parseInt(user.getMaxId()))
                        .tokenExpirationInSeconds(86400)
                        .tokenType(requestDto.getTokenType())
                        .build();

        return agoraService.generateToken(agoraTokenRequestDto)
                .doOnError(throwable -> log.error("Error happened while generating Agora Token : {}", throwable.getMessage()))
                .map(agoraTokenResponseDto -> {
                    if (agoraTokenResponseDto.getData() != null && !agoraTokenResponseDto.getData().isEmpty()) {
                        roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getAgoraToken());
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

    private Mono<StreamResponseDto> buildJoinAudioStreamResponseDto(LiveRoomViewerRequestDto requestDto, LiveRoom liveRoom, User user, String message, Boolean maxParticipantsReached) {
        RoomDataDto roomDataDto = new RoomDataDto();
        roomDataDto.setId(liveRoom.getId());
        roomDataDto.setHostMaxId(liveRoom.getHostMaxId());
        roomDataDto.setJoinedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
//        roomDataDto.setAnnouncement(liveRoom.getAnnouncement());
        roomDataDto.setWelcomeAnnouncement(liveRoom.getWelcomeAnnouncement());
        roomDataDto.setViewerMaxId(user.getMaxId());
        roomDataDto.setMaxAudioParticipants(liveRoom.getMaxAudioParticipants());
        roomDataDto.setAudioParticipants(liveRoom.getAudioParticipants());

        AgoraTokenRequestDto agoraTokenRequestDto =
                AgoraTokenRequestDto
                        .builder()
                        .channelName(liveRoom.getId())
                        .role(maxParticipantsReached ? AgoraTokenTypeEnum.ROLE_SUBSCRIBER.getValue() : AgoraTokenTypeEnum.ROLE_PUBLISHER.getValue())
                        .uid(Integer.parseInt(user.getMaxId()))
                        .tokenExpirationInSeconds(86400)
                        .tokenType(requestDto.getTokenType())
                        .build();

        log.info("Token role : {}", agoraTokenRequestDto.getRole());

        return agoraService.generateToken(agoraTokenRequestDto)
                .doOnError(throwable -> log.error("Error happened while generating Agora Token : {}", throwable.getMessage()))
                .map(agoraTokenResponseDto -> {
                    if (agoraTokenResponseDto.getData() != null && !agoraTokenResponseDto.getData().isEmpty()) {
                        roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getAgoraToken());
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
        roomDataDto.setCreatedOn(liveRoom.getCreatedOn());
        roomDataDto.setCountry(liveRoom.getCountry());
        roomDataDto.setViewer(liveRoom.getViewer());
        roomDataDto.setHostDailyGems(liveRoom.getHostDailyGems());
        roomDataDto.setMaxAudioParticipants(liveRoom.getMaxAudioParticipants());
        roomDataDto.setAudioParticipants(liveRoom.getAudioParticipants());

        roomDataDto.setAudioSeatNumber(liveRoom.getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())
                ? liveRoom.getAudioSeatNumber()
                : null);
        roomDataDto.setAudioSkinId(Strings.isNotNullAndNotEmpty(liveRoom.getAudioSkinId()) ? liveRoom.getAudioSkinId() : "");
        roomDataDto.setAudioSkinUrl(Strings.isNotNullAndNotEmpty(liveRoom.getAudioSkinUrl()) ? liveRoom.getAudioSkinUrl() : "");

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
                        roomDataDto.setAgoraToken(agoraTokenResponseDto.getData().get(0).getAgoraToken());
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
        roomDataDto.setLeftOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
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
        roomDataDto.setCommentedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
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
        joinPermission.setAutoJoinEnabled(liveRoom.getEnableAutoJoin());
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
                                    announcement.setTime(ZonedDateTime.now(ZoneOffset.UTC).toString());

                                    List<String> imageUrlList =
                                            content.getResourceFormats() != null
                                                    ?
                                                    content.getResourceFormats()
                                                            .stream()
                                                            .filter(resourceFormat -> resourceFormat.getResourceType().equals("IMAGE"))
                                                            .map(ResourceFormat::getResourceUrl).toList()
                                                    :
                                                    List.of();

                                    Announcement.Resource resource = Announcement.Resource
                                            .builder()
                                            .name(content.getName())
                                            .imageUrl(!imageUrlList.isEmpty() ? imageUrlList.get(0) : null)
                                            .build();

                                    List<Announcement.Resources> resources = GiftTransactionService.buildResourceCollectionRide(content);

                                    announcement.setRide(
                                            Announcement.Ride
                                                    .builder()
                                                    .id(content.getId())
                                                    .name(content.getName())
                                                    .resource(resource)
                                                    .resources(resources)
                                                    .build()
                                    );

//                                    announcement.setResource(
//                                            Announcement.Resource
//                                                    .builder()
//                                                    .name(content.getName())
//                                                    .imageUrl(!imageUrlList.isEmpty() ? imageUrlList.get(0) : null)
//                                                    .build()
//                                    );
                                    return Tuples.of(announcement, user);
                                });
                    } else if (Strings.isNotNullAndNotEmpty(user.getEntryCardId())) {
                        return contentUseCase.getContentById(user.getEntryCardId())
                                .map(content -> {
                                    announcement.setAnnouncementId(UUID.randomUUID().toString());
                                    announcement.setMessageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_ENTRY_CARD.getValue()));
                                    announcement.setType(AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_ENTRY_CARD.getValue());
                                    announcement.setTime(ZonedDateTime.now(ZoneOffset.UTC).toString());
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
                        announcement.setTime(ZonedDateTime.now(ZoneOffset.UTC).toString());
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
        announcement.setTime(ZonedDateTime.now(ZoneOffset.UTC).toString());
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
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,
                        "No LiveRoom found with Id : " + requestDto.getLiveRoomId())))
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .flatMap(liveRoom ->
                        userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                                .flatMap(user -> {
                                    if (OfficialIdEnum.INVISIBLE_IDS.getValue().contains(user.getMaxId())) {
                                        log.info("Invisible user (maxId: {}) left silently.", user.getMaxId());
                                        return buildLeaveStreamResponseDto(liveRoom,
                                                "Invisible user left silently.");
                                    }
                                    return this.updateLiveRoomForFanLeave(liveRoom, user)
                                            .flatMap(updatedRoom -> port.saveLiveRoom(updatedRoom)
                                                    .thenReturn(updatedRoom))
                                            .doOnNext(updatedRoom -> cachePort.updateForViewerLeave(updatedRoom)
                                                    .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                                                    .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                                                    .subscribeOn(Schedulers.boundedElastic()).subscribe())
                                            .flatMap(updatedRoom -> this.buildLeaveStreamResponseDto(updatedRoom,
                                                    "User successfully left the live room."));
                                }))
                .doOnError(throwable -> log.error("Failed to Update LiveRoom with fan Leave. Error : {}", throwable.getMessage()));
    }

    @Override
    public Mono<StreamResponseDto> endStream(String liveRoomId, String keycloakId) {
        List<String> superUsers = OfficialIdEnum.OFFICIAL_IDS.getValue();
        return port.getLiveRoomById(liveRoomId)
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + liveRoomId)))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot End.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(keycloakId)
                        .filter(user -> liveRoom.getUserId().equals(user.getId()) || superUsers.contains(user.getMaxId()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not the host of the LiveRoom or official one. Cannot end.")))
                        .map(user -> liveRoom))
                .map(liveRoom -> {
                    liveRoom.setStatus(Constants.STATUS_OFFLINE.getValue());
                    liveRoom.setEndedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
                    liveRoom.setDurationInSeconds((liveRoom.getEndedOn().getEpochSecond() - liveRoom.getCreatedOn().getEpochSecond()));
                    return liveRoom;
                })
                .flatMap(liveRoom -> giftTransactionPersistencePort.getTotalGiftByLiveRoomId(liveRoomId)
                        .map(totalReceivedGift -> {
                            liveRoom.setGiftReceivedAmount(totalReceivedGift);
                            int month = ZonedDateTime.now(ZoneOffset.UTC).getMonthValue();
                            int year = ZonedDateTime.now(ZoneOffset.UTC).getYear();
                            liveRoom.setMonth(month);
                            liveRoom.setYear(year);
                            return liveRoom;
                        })
                )
                .flatMap(port::saveLiveRoom)
                .doOnNext(liveRoom -> {
                    log.info("LiveRoom details: {}", liveRoom);
                    liveRoomSummaryUseCase.processLiveRoomSummary(liveRoom)
                            .doOnNext(liveRoomSummary -> log.info("LiveRoom Summary processed successfully"))
                            .doOnNext(liveRoomSummaryEntity -> log.debug("LiveRoomSummary details: {}", liveRoomSummaryEntity))
                            .doOnError(throwable -> log.error("Error Happened while processing LiveRoom Summary : {}", throwable.getMessage()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                })
                .doOnNext(liveRoom ->
                        cachePort.updateForEndStream(liveRoom)
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
                                .flatMap(user -> {
                                    if (OfficialIdEnum.KICKED_OUT_IDS.getValue().contains(user.getMaxId())) {
                                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Kickout not possible, Official ID"));
                                    }
                                    return Mono.just(user);
                                })
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
            List<String> kickedOutUserIds = liveRoom.getKickedOutUserIds();
            if (!kickedOutUserIds.contains(kickedUser.getId())) {
                kickedOutUserIds.add(kickedUser.getId());
            }
            liveRoom.setKickedOutUserIds(kickedOutUserIds);
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
                .profileFrameUrl(kickedUser.getProfileFrameUrl())
                .profileFrameId(kickedUser.getProfileFrameId())
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
        String viewMode = requestDto.getViewMode();
        String country = requestDto.getCountry();
        String keycloakId = requestDto.getKeycloakId();
        Pageable pageable = requestDto.getPageable();
        String mediaType = requestDto.getMediaType();

        if (viewMode.equals(Constants.VIEW_MODE_SK.getValue()) || viewMode.equals(Constants.VIEW_MODE_GUEST_CALL.getValue())) {
            return Mono.just(LiveRoomGridViewResponseDto.builder()
                    .userMessage("No data available for SK or GUEST_CALL view.")
                    .data(Collections.emptyList())
                    .count(0)
                    .build());
        }

        return resolveRoomsAndCount(viewMode, keycloakId, country, pageable, mediaType)
                .map(tuple -> LiveRoomGridViewResponseDto.builder()
                        .userMessage("LiveRoom Grid View Fetched Successfully.")
                        .count(tuple.getT1().intValue())
                        .data(buildLiveRoomResponse(tuple.getT2()))
                        .build());
    }

    private Mono<Tuple2<Long, List<LiveRoom>>> resolveRoomsAndCount(String viewMode, String keycloakId, String country, Pageable pageable, String mediaType) {
        if (viewMode.equals(Constants.VIEW_MODE_FOLLOWING.getValue())) {
            return Mono.zip(
                    port.getFollowingLiveRoomsCount(keycloakId, mediaType),
                    port.getFollowingLiveRooms(keycloakId, pageable, mediaType)
            );
        } else if (viewMode.equals(Constants.VIEW_MODE_EXPLORE.getValue())) {
            return Mono.zip(
                    port.getActiveLiveRoomsCountByTypeAndCountry(mediaType, country, viewMode),
                    port.getActiveVideoAndAudioLiveRooms(pageable, country, mediaType).collectList()
            );
        }
        return Mono.zip(
                port.getActiveLiveRoomsCountByTypeAndCountry(mediaType, null, viewMode),
                port.getActiveVideoAndAudioLiveRooms(pageable, null, mediaType).collectList()
        );
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
                                            .time(ZonedDateTime.now(ZoneOffset.UTC).toInstant().toString())
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
    public Mono<LiveRoomJoinPermissionResponseDto> setJoinSettings(JoinSettingsRequestDto joinSettingsRequestDto) {
        if (Strings.isNullOrEmpty(joinSettingsRequestDto.getMediaType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Media Type is required."));
        }
        if (Strings.isNullOrEmpty(joinSettingsRequestDto.getLiveRoomId())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is required."));
        }
        if (Strings.isNullOrEmpty(joinSettingsRequestDto.getMode())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Mode Type is required."));
        }
        if (Strings.isNullOrEmpty(joinSettingsRequestDto.getStatus())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Status is required."));
        }

        String mediaType = joinSettingsRequestDto.getMediaType();
        String liveRoomId = joinSettingsRequestDto.getLiveRoomId();
        String mode = joinSettingsRequestDto.getMode();
        String status = joinSettingsRequestDto.getStatus();

        List<String> validMediaType = List.of(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue(), Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue());
        if (!validMediaType.contains(mediaType)) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid media type!"));
        }

        List<String> validModeType = List.of(Constants.MODE_APPROVAL.getValue(), Constants.MODE_AUTO.getValue());
        if (!validModeType.contains(mode)) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid mode type!"));
        }

        if (mediaType.equals(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue()) && mode.equals(Constants.MODE_AUTO.getValue())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Auto Mode is not available for video media."));
        }
        if (mediaType.equals(Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue()) && mode.equals(Constants.MODE_APPROVAL.getValue())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Approval Mode is not available for Audio media."));
        }

        JoinPermissionRequestDTO joinPermissionRequestDTO = JoinPermissionRequestDTO.builder()
                .liveRoomId(liveRoomId)
                .keycloakId(joinSettingsRequestDto.getKeycloakId())
                .build();

        if (mediaType.equals(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue())) {
            joinPermissionRequestDTO.setEnableJoin(status);
            return this.setJoinPermission(joinPermissionRequestDTO);
        }

        joinPermissionRequestDTO.setEnableAutoJoin(status);
        return this.setEnableAutoJoinAudioStream(joinPermissionRequestDTO);

    }

    @Override
    public Mono<LiveRoomJoinPermissionResponseDto> setJoinPermission(JoinPermissionRequestDTO requestDTO) {
        return port.getLiveRoomById(requestDTO.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom does not exist by the given id")))
                .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not live")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDTO.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                        .map(User::getId)
                        .doOnSuccess(userId -> log.info("User Id : {}", userId))
                        .filter(userId -> liveRoom.getUserId().equals(userId))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User must be the host of the LiveRoom to set the permission")))
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
                .flatMap(liveRoom -> this.buildEnableJoinResponseDTO(liveRoom, "Join call setting updated successfully."))
                .doOnError(throwable -> log.error("Error Happened while setting join permission: {}", throwable.getMessage()));
    }

    @Override
    public Mono<JoinCallResponseDto> requestJoinCall(JoinCallRequestDto requestDto) {
        List<String> validMedia = List.of(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue(), Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue());
        if (!validMedia.contains(requestDto.getMediaType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid media type!"));
        }
        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found")))
                .zipWith(userUseCase.getUserByKeycloakId(requestDto.getKeycloakId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                .filter(tupleOfLiveRoomAndUser -> tupleOfLiveRoomAndUser.getT2().getActive().equalsIgnoreCase(Constants.STATUS_YES.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "The user is the host of the LiveRoom and cannot join as a participant.")))
                .flatMap(tupleOfLiveRoomAndUser -> {
                    LiveRoom liveRoom = tupleOfLiveRoomAndUser.getT1();
                    User user = tupleOfLiveRoomAndUser.getT2();
                    boolean isUserKickedOut = liveRoom.getKickedOutUserIds().stream()
                            .anyMatch(s -> s.equalsIgnoreCase(user.getId()));
                    if (isUserKickedOut) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "The user has been kicked out of the LiveRoom and cannot rejoin as a participant."));
                    } else
                        return Mono.just(tupleOfLiveRoomAndUser); // Allows the liveRoom to pass through the filter if the user is not kicked out
                })
                .filter(liveRoomUserTuple2 -> Objects.nonNull(liveRoomUserTuple2.getT1().getEnableJoin()) && Constants.STATUS_YES.getValue().equalsIgnoreCase(liveRoomUserTuple2.getT1().getEnableJoin()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join call is not enabled for the LiveRoom")))
                .filter(liveRoomUserTuple2 -> liveRoomUserTuple2.getT1().getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not live")))
                .filter(liveRoomUserTuple2 -> liveRoomUserTuple2.getT1().getViewerIds() != null
                        && !liveRoomUserTuple2.getT1().getViewerIds().isEmpty()
                        && liveRoomUserTuple2.getT1().getViewerIds().stream().anyMatch(s -> s.equalsIgnoreCase(liveRoomUserTuple2.getT2().getId())))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not a viewer of the LiveRoom and cannot join as a participant.")))
                .flatMap(tupleOfLiveRoomAndUser ->
                {
//                    log.info("live room {} and userInfo : {}", tupleOfLiveRoomAndUser.getT1(), tupleOfLiveRoomAndUser.getT2());

                    if (requestDto.getMediaType().equals(Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue())) {
                        requestDto.setCameraOn(null);
                        requestDto.setCameraView(null);
                    }
                    return buildJoinRequest(tupleOfLiveRoomAndUser.getT2(), requestDto, tupleOfLiveRoomAndUser.getT1())
                            .flatMap(joinRequests -> {
                                LiveRoom liveRoom = tupleOfLiveRoomAndUser.getT1();
                                liveRoom.setJoinRequests(joinRequests);
                                return port.saveLiveRoom(liveRoom).zipWith(Mono.just(tupleOfLiveRoomAndUser.getT2()));
                            });
                })
                .flatMap(liveRoomUserTuple2 ->
                        cachePort.updateForJoinRequest(liveRoomUserTuple2.getT1())
                                .zipWith(Mono.just(liveRoomUserTuple2.getT2()), (liveRoomEntity, userEntity) -> {
                                    log.info("LiveRoom updated into Firebase successfully");
                                    return Tuples.of(liveRoomEntity, userEntity);
                                })
                                .doOnError(throwable -> log.error("Error happened while updating LiveRoom into Firebase: {}", throwable.getMessage()))
                                .subscribeOn(Schedulers.boundedElastic())
                )
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
        List<String> validMedia = List.of(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue(), Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue());
        if (!validMedia.contains(requestDto.getMediaType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid media type!"));
        }

        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found by the given id")))
                .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .filter(liveRoom -> liveRoom.getEnableJoin().equals(Constants.STATUS_YES.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not live")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                        .filter(user -> user.getActive().equalsIgnoreCase(Constants.STATUS_YES.getValue()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not active")))
                        .filter(user -> liveRoom.getUserId().equals(user.getId()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not the host of the LiveRoom")))
                        .thenReturn(liveRoom))
                .flatMap(liveRoom -> {
                    List<String> validTypes = Arrays.asList(Constants.STATUS_APPROVED.getValue(), Constants.STATUS_DECLINE.getValue());

                    if (!validTypes.contains(requestDto.getAction())) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid Action Type!"));
                    }

                    return Mono.just(liveRoom);
                })
                .flatMap(liveRoom -> cachePort.getLiveRoomById(liveRoom.getId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found by the given id")))
                        .flatMap(liveRoomEntity -> {

                            if (requestDto.getMediaType().equals(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue())) {
                                if (!liveRoomEntity.getEnableJoin().equals(Constants.STATUS_YES.getValue())) {
                                    return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Enable join value is No currently for Video media!"));
                                }
                            }
                            if (requestDto.getMediaType().equals(Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue())) {
                                if (!liveRoomEntity.getEnableJoin().equals(Constants.STATUS_YES.getValue())
                                        || !liveRoomEntity.getEnableAutoJoin().equals(Constants.STATUS_NO.getValue())
                                ) {
                                    return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Enable join value isn't Yes OR enable auto join value isn't No currently for audio media"));
                                }
                            }

                            if (liveRoomEntity.getJoinRequests() == null || liveRoomEntity.getJoinRequests().isEmpty()) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No Join Requests found for the LiveRoom"));
                            } else if (liveRoomEntity.getJoinRequests().stream().noneMatch(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join Request not found for the LiveRoom"));
                            }

                            List<String> existsAsKickedOutUser = liveRoomEntity.getJoinRequests().stream()
                                    .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                                    .map(JoinRequests::getUserId)
                                    .filter(joinRequestUserId -> liveRoom.getKickedOutUserIds().contains(joinRequestUserId))
                                    .toList();

                            List<String> existsAsViewer = liveRoomEntity.getJoinRequests().stream()
                                    .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                                    .map(JoinRequests::getUserId)
                                    .filter(joinRequestUserId -> liveRoom.getViewerIds().contains(joinRequestUserId))
                                    .toList();

                            if (!existsAsKickedOutUser.isEmpty()) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is kicked out of the LiveRoom and cannot join as a participant."));
                            }
                            if (existsAsViewer.isEmpty()) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not a viewer of the LiveRoom and cannot join as a participant."));
                            }

                            if (requestDto.getAction().equals(Constants.STATUS_APPROVED.getValue())) {
                                long approvedOrStartedCount = liveRoomEntity.getJoinRequests().stream()
                                        .map(JoinRequests::getStatus)
                                        .filter(status -> status.equals(Constants.STATUS_APPROVED.getValue()) || status.equals(Constants.STATUS_STARTED.getValue()))
                                        .count();

                                if (liveRoomEntity.getType().equals(Constants.LIVE_ROOM_TYPE_VIDEO.getValue()) && approvedOrStartedCount >= 3) {
                                    return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Maximum 3 participants are allowed to join call."));
                                } else if (liveRoomEntity.getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())) {
//                                    int maxParticipants = liveRoomEntity.getHost().getUserLevel() <= 20 ? 8 : 15;
                                    int maxParticipants = 10;
                                    if (approvedOrStartedCount >= maxParticipants) {
                                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Maximum " + maxParticipants + " participants are allowed to join call."));
                                    }
                                }
                            }

                            return Mono.just(liveRoomEntity);
                        })
                        .map(liveRoomEntity -> liveRoom))
                .doOnNext(liveRoom -> cachePort.updateForProcessingJoinCall(liveRoom, requestDto)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error happened while updating LiveRoom into Firebase: {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .map(liveRoom -> buildJoinCallProcessData(liveRoom, requestDto, requestDto.getAction()))
                .map(liveRoomJoinRequestInfo -> JoinCallResponseDto.builder()
                        .message("Join call request processed successfully.")
                        .data(liveRoomJoinRequestInfo)
                        .count(1)
                        .error(false)
                        .build());
    }

    @Override
    public Mono<JoinCallResponseDto> startJoinCall(JoinCallRequestDto requestDto) {
        List<String> validMedia = List.of(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue(), Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue());
        if (!validMedia.contains(requestDto.getMediaType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid media type!"));
        }
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
                .flatMap(liveRoom -> cachePort.getLiveRoomById(liveRoom.getId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found by the given id")))
                        .flatMap(liveRoomEntity -> {

                            if (requestDto.getMediaType().equals(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue())) {
                                if (!liveRoomEntity.getEnableJoin().equals(Constants.STATUS_YES.getValue())) {
                                    return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Enable join value is No currently for Video media!"));
                                }
                            }
                            if (requestDto.getMediaType().equals(Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue())) {
                                if (!liveRoomEntity.getEnableJoin().equals(Constants.STATUS_YES.getValue())
                                        || !liveRoomEntity.getEnableAutoJoin().equals(Constants.STATUS_NO.getValue())
                                ) {
                                    return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Enable join value isn't Yes OR enable auto join value isn't No currently for audio media"));
                                }
                            }

                            if (liveRoomEntity.getJoinRequests() == null || liveRoomEntity.getJoinRequests().isEmpty()) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No Join Requests found for the LiveRoom"));
                            } else if (liveRoomEntity.getJoinRequests().stream().noneMatch(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join Request not found for the LiveRoom"));
                            } else if (liveRoomEntity.getJoinRequests().stream().noneMatch(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId())
                                    && joinRequests.getStatus().equals(Constants.STATUS_APPROVED.getValue()))) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join Request is not approved for the LiveRoom"));
                            }

                            return Mono.just(liveRoomEntity);
                        })
                        .map(liveRoomEntity -> liveRoom))
                .doOnNext(liveRoom -> cachePort.updateForStartingJoinCall(liveRoom, requestDto)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error happened while updating LiveRoom into Firebase: {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .map(liveRoom -> buildJoinCallProcessData(liveRoom, requestDto, Constants.STATUS_STARTED.getValue()))
                .map(liveRoomJoinRequestInfo -> JoinCallResponseDto.builder()
                        .message("Start Join call request processed successfully.")
                        .data(liveRoomJoinRequestInfo)
                        .count(1)
                        .error(false)
                        .build());
    }

    @Override
    public Mono<JoinCallResponseDto> closeJoinedCall(JoinCallRequestDto requestDto) {
        AtomicReference<String> userMaxId = new AtomicReference<>();
        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found by the given id")))
                .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not live")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                        .filter(user -> user.getActive().equalsIgnoreCase(Constants.STATUS_YES.getValue()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not active")))
                        .map(user -> Tuples.of(liveRoom, user)))
                .flatMap(liveRoomAndUserTuple -> {
                    LiveRoom liveRoom = liveRoomAndUserTuple.getT1();
                    User user = liveRoomAndUserTuple.getT2();
                    userMaxId.set(user.getMaxId());
                    return cachePort.getLiveRoomById(liveRoom.getId())
                            .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found by the given id")))
                            .flatMap(liveRoomEntity -> this.validateCloseJoinedCallRequest(liveRoomEntity, requestDto, user))
                            .map(liveRoomEntity -> liveRoom);
                })
                .doOnError(throwable -> log.error("Error happened while fetching & validating request: {}", throwable.getMessage()))
                .doOnNext(liveRoom -> cachePort.updateForClosingJoinedCall(liveRoom, requestDto)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error happened while updating LiveRoom into Firebase: {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .flatMap(liveRoom -> {
                    LiveRoomJoinRequestInfo closeJoinRequestDto = buildJoinCallProcessData(liveRoom, requestDto, Status.STATUS_CLOSED.getValue());
                    return this.getAgoraToken(liveRoom.getId(), AgoraTokenTypeEnum.ROLE_SUBSCRIBER.getValue(), userMaxId.get(), AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue())
                            .map(agoraToken -> {
                                closeJoinRequestDto.setAgoraToken(agoraToken);
                                return closeJoinRequestDto;
                            });
                })
                .doOnError(throwable -> log.error("Error happened while generating Agora Token: {}", throwable.getMessage()))
                .map(liveRoomJoinRequestInfo -> JoinCallResponseDto.builder()
                        .message("Joined call closed successfully.")
                        .data(liveRoomJoinRequestInfo)
                        .count(1)
                        .error(false)
                        .build())
                .doOnError(throwable -> log.error("Error happened while closing joined call: {}", throwable.getMessage()));
    }

    @Override
    public Mono<StreamResponseDto> createAudioStream(LiveRoomRequestDto requestDto) {
        Integer seatNumber = requestDto.getAudioSeatNumber() == null
                ? AllowedSeatNumber.SEAT_5.getSeatNumber()
                : requestDto.getAudioSeatNumber();

        if (!AllowedSeatNumber.isValid(seatNumber)) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid seat number: " + seatNumber));
        }

        requestDto.setAudioSeatNumber(seatNumber);
        AtomicReference<String> liveRoomId = new AtomicReference<>();
        return userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                .doOnNext(user -> log.info("User received : {}", user))
                .doOnError(throwable -> log.error("Error happened while retrieving user : {}", throwable.getMessage()))
                .filter(user -> user.getUserType().equals(UserTypeEnum.USER_TYPE_HOST.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User must be a host to create LiveRoom.")))
                .flatMap(user -> {
                    String audioSkinId = requestDto.getAudioSkinId();
                    if (audioSkinId == null || audioSkinId.trim().isEmpty()) {
                        return Mono.just(user);
                    }
                    return contentUseCase.getContentByContentIdAndUserId(audioSkinId, user.getId())
                            .filter(bagEntity -> bagEntity.getType().equalsIgnoreCase(ContentTypeEnum.AUDIO_SKIN.getValue()))
                            .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Content type doesn't match!")))
                            .thenReturn(user);
                })
                .flatMap(user -> hostUseCase.getHostByUserId(user.getId())
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
                                .thenReturn(Tuples.of(user, host))))
                .flatMap(userHostTuple2 -> this.buildAudioLiveRoomDomain(userHostTuple2.getT2(), requestDto)
                        .flatMap(port::saveLiveRoom)
                        .map(liveRoom -> {
                            liveRoomId.set(liveRoom.getId());
                            return liveRoom;
                        })
                        .doOnSuccess(liveRoom -> log.info("LiveRoom saved into db"))
                        .doOnError(throwable -> log.error("Error happened while saving LiveRoom into db : {}", throwable.getMessage()))
                        .flatMap(liveRoom -> {
                            String audioSkinId = requestDto.getAudioSkinId();
                            if (audioSkinId == null || audioSkinId.trim().isEmpty()) {
                                return Mono.just(liveRoom);
                            }
                            return this.findContentAndReturnResource(liveRoom.getAudioSkinId())
                                    .flatMap(url -> {
                                        liveRoom.setAudioSkinUrl(url);
                                        return Mono.just(liveRoom);
                                    });
                        })
                        .doOnNext(liveRoom -> this.buildFirebaseEntity(liveRoom, userHostTuple2.getT2(), userHostTuple2.getT1())
                                .flatMap(cachePort::create)
                                .doOnNext(firebaseEntity -> log.info("LiveRoom saved into firebase successfully"))
                                .doOnError(throwable -> log.error("Error Happened while saving LiveRoom into Firebase : {}", throwable.getMessage()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe()))
                .flatMap(liveRoom -> this.buildCreateStreamResponseDto(requestDto, liveRoom, "Audio Live room created successfully."))
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

    private Mono<String> findContentAndReturnResource(String audioSkinId) {
        return contentUseCase.getContentById(audioSkinId)
                .flatMap(content -> content.getResourceFormats().stream()
                        .filter(format -> Constants.RESOURCES_TYPE_IMAGE.getValue()
                                .equalsIgnoreCase(format.getResourceType()))
                        .map(ResourceFormat::getResourceUrl)
                        .findFirst()
                        .map(Mono::just)
                        .orElse(Mono.just(""))
                );
    }


    @Override
    public Mono<StreamResponseDto> joinAudioStream(LiveRoomViewerRequestDto liveRoomViewerRequestDto) {
        List<String> validTokenTypes = List.of(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_UID_AND_PRIVILEGE.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_RTM.getValue());
        if (!validTokenTypes.contains(liveRoomViewerRequestDto.getTokenType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid token type"));
        }
        AtomicReference<Boolean> audioParticipantLimitReached = new AtomicReference<>(false);

        return port.getLiveRoomById(liveRoomViewerRequestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + liveRoomViewerRequestDto.getLiveRoomId())))
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot join.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(liveRoomViewerRequestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User not found!")))
                        .flatMap(commonBusiness::setUserLevelUrl)
                        .flatMap(user -> {

                            if (OfficialIdEnum.INVISIBLE_IDS.getValue().contains(user.getMaxId())) {
                                log.info("Invisible user (maxId: {}) joined silently.", user.getMaxId());
                                return buildJoinAudioStreamResponseDto(liveRoomViewerRequestDto, liveRoom, user,
                                        "Invisible user joined silently.", true);
                            }

                            return this.joinUserAudioStream(liveRoom, user, liveRoomViewerRequestDto, audioParticipantLimitReached)
                                    .doOnRequest(liveRoomEntity -> log.info("Requesting to join user into LiveRoom"))
                                    .doOnSuccess(liveRoomEntity -> log.info("User successfully joined into LiveRoom"))
                                    .doOnError(throwable -> log.error("Error Happened while joining user into LiveRoom : {}", throwable.getMessage()));
                        }))

                .doOnError(throwable -> log.error("Failed to Update LiveRoom with fan Entry. Error : {}", throwable.getMessage()));
    }

    private Mono<StreamResponseDto> joinUserAudioStream(LiveRoom liveRoom, User user, LiveRoomViewerRequestDto liveRoomViewerRequestDto, AtomicReference<Boolean> audioParticipantLimitReached) {
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
                .profileFrameUrl(user.getProfileFrameUrl())
                .profileFrameId(user.getProfileFrameId())
                .userLevel(user.getUserLevel())
                .levelBadgeUrl(user.getLevelBadgeUrl())
                .build();
        liveRoom.setViewer(viewer);
        liveRoom.setViewerCount(liveRoom.getViewerCount() + 1);

//                            log.info("audio participants size : {}", liveRoom.getAudioParticipants().size());
        if (liveRoom.getAudioParticipants().size() < liveRoom.getMaxAudioParticipants()) {
            List<Viewer> updatedAudioParticipants = new ArrayList<>(liveRoom.getAudioParticipants());
            updatedAudioParticipants.add(viewer);
            liveRoom.setAudioParticipants(updatedAudioParticipants);
        } else {
            audioParticipantLimitReached.set(true);
        }

        List<String> currentViewerIdsInLiveroom = new ArrayList<>(liveRoom.getViewerIds() != null && !liveRoom.getViewerIds().isEmpty()
                ? liveRoom.getViewerIds() : new ArrayList<>());
        currentViewerIdsInLiveroom.add(viewer.getUserId());
        liveRoom.setViewerIds(currentViewerIdsInLiveroom);
        liveRoom.setTotalViewerCount(liveRoom.getTotalViewerCount() + 1);

        return Mono.just(liveRoom)
                .flatMap(liveRoom1 -> {
                    if (liveRoom1.getKickedOutUserIds() == null || liveRoom1.getKickedOutUserIds().isEmpty()) {
                        liveRoom1.setKickedOutUserIds(new ArrayList<>());
                    }
                    return Mono.just(liveRoom1);
                })
                .filter(liveRoom1 -> !liveRoom1.getKickedOutUserIds().contains(liveRoom1.getViewer().getUserId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is kicked out from LiveRoom. Cannot join.")))
                .flatMap(this::buildJoinAnnouncement)
                .doOnNext(liveRoom1 -> port.saveLiveRoom(liveRoom1)
                        .flatMap(liveRoom2 -> cachePort.update(liveRoom1))
                        .doOnRequest(liveRoomEntity -> log.info("Requesting to update LiveRoom into firebase"))
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .flatMap(liveRoom1 -> this.buildJoinAudioStreamResponseDto(liveRoomViewerRequestDto, liveRoom1, user, "User has successfully joined the room.", audioParticipantLimitReached.get()));
    }

    @Override
    public Mono<LiveRoomJoinPermissionResponseDto> setEnableAutoJoinAudioStream(JoinPermissionRequestDTO requestDTO) {
        log.info("Request received to set auto join audio stream with details : {}", requestDTO);
        return port.getLiveRoomById(requestDTO.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom does not exist by the given id")))
                .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not live")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDTO.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                        .map(User::getId)
                        .doOnSuccess(userId -> log.info("User Id : {}", userId))
                        .filter(userId -> liveRoom.getUserId().equals(userId))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User must be the host of the LiveRoom to set the permission")))
                        .thenReturn(liveRoom))
                .flatMap(liveRoom -> {
                    List<String> validTypes = Arrays.asList(Constants.STATUS_YES.getValue(), Constants.STATUS_NO.getValue());

                    if (requestDTO.getEnableAutoJoin() != null && !validTypes.contains(requestDTO.getEnableAutoJoin())) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid EnableAutoJoin Type!"));
                    }

//                    if (requestDTO.getEnableJoin() != null && !validTypes.contains(requestDTO.getEnableJoin())) {
//                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid EnableJoin Type!"));
//                    }

//                    liveRoom.setEnableJoin(requestDTO.getEnableJoin());
                    liveRoom.setEnableAutoJoin(requestDTO.getEnableAutoJoin());
                    return port.saveLiveRoom(liveRoom);
                })
                .doOnNext(liveRoom1 -> cachePort.updateForAutoJoinPermission(liveRoom1)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe())
                .flatMap(liveRoom -> this.buildEnableJoinResponseDTO(liveRoom, "Auto Join AudioStream updated successfully."))
                .map(liveRoomJoinPermissionResponseDto -> {
                    liveRoomJoinPermissionResponseDto.getData().setJoinCallAvailable(null);
                    return liveRoomJoinPermissionResponseDto;
                })
                .doOnError(throwable -> log.error("Error Happened while setting join permission: {}", throwable.getMessage()));
    }


    private Mono<LiveRoomFirebaseEntity> validateCloseJoinedCallRequest(LiveRoomFirebaseEntity liveRoomEntity, JoinCallRequestDto requestDto, User user) {
        if (liveRoomEntity.getJoinRequests() == null || liveRoomEntity.getJoinRequests().isEmpty()) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No Join Requests found for the LiveRoom"));
        } else if (liveRoomEntity.getJoinRequests().stream().noneMatch(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join Request Id doesn't match with any join Request of the LiveRoom"));
        }

        List<String> validPersonRequested = liveRoomEntity.getJoinRequests().stream()
                .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                .map(JoinRequests::getUserId)
                .filter(joinRequestUserId -> user.getId().equals(joinRequestUserId) || liveRoomEntity.getHost().getUserId().equals(joinRequestUserId))
                .toList(); //If Host or Co-host

        List<String> validStatus = liveRoomEntity.getJoinRequests().stream()
                .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                .map(JoinRequests::getStatus)
                .filter(joinRequestStatus -> joinRequestStatus.equals(Constants.STATUS_APPROVED.getValue()) || joinRequestStatus.equals(Constants.STATUS_STARTED.getValue()))
                .toList();

        if (validPersonRequested.isEmpty() && !liveRoomEntity.getHost().getUserId().equals(user.getId())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Request cannot be processed!"));
        }

        if (validStatus.isEmpty()) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join Request is not Approved !"));
        }

        return Mono.just(liveRoomEntity);
    }

    private Mono<String> getAgoraToken(String channelName, String role, String uid, String tokenType) {
        AgoraTokenRequestDto agoraTokenRequestDto =
                AgoraTokenRequestDto
                        .builder()
                        .channelName(channelName)
                        .role(role)
                        .uid(Integer.parseInt(uid))
                        .tokenExpirationInSeconds(86400)
                        .tokenType(tokenType)
                        .build();

        return agoraService.generateToken(agoraTokenRequestDto)
                .doOnError(throwable -> log.error("Error happened while generating Agora Token : {}", throwable.getMessage()))
                .map(agoraTokenResponseDto -> {
                    if (agoraTokenResponseDto.getData() != null && !agoraTokenResponseDto.getData().isEmpty()) {
                        return agoraTokenResponseDto.getData().get(0).getAgoraToken();
                    }
                    return null;
                })
                .onErrorMap(throwable -> new ExceptionHandlerUtil(HttpStatus.INTERNAL_SERVER_ERROR, "Error happened while generating Agora Token!"));
    }

    private LiveRoomJoinRequestInfo buildJoinCallProcessData(LiveRoom liveRoom, JoinCallRequestDto requestDto, String status) {
        return LiveRoomJoinRequestInfo.builder()
                .roomId(requestDto.getLiveRoomId())
                .status(status)
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

    private Mono<List<JoinRequests>> buildJoinRequest(User user, JoinCallRequestDto requestDto, LiveRoom liveRoom) {
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
                            .gender(user.getGender())
                            .profileFrameId(user.getProfileFrameId())
                            .profileFrameUrl(user.getProfileFrameUrl())
                            .build();
                    joinRequest.setSeatIndex(requestDto.getSeatNumber() != null ? requestDto.getSeatNumber() : -1);
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
        return giftTransactionPersistencePort.getTotalGiftByLiveRoomId(liveRoom.getId())
                .defaultIfEmpty(0.0)
                .map(giftReceivedAmount -> StreamResponseDto
                        .builder()
                        .message("LiveStream Ended Successfully")
                        .data(RoomDataDto
                                .builder()
                                .id(liveRoom.getId())
                                .endedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant())
                                .durationInSeconds(liveRoom.getDurationInSeconds())
                                .duration(FormatUtil.convertDurationToString(liveRoom.getDurationInSeconds()))
                                .totalViewerCount(liveRoom.getTotalViewerCount())
                                .hostDailyGems(liveRoom.getHostDailyGems())
                                .giftReceivedAmount(giftReceivedAmount)
                                .giftReceivedAmountString(FormatUtil.formatGems(giftReceivedAmount))
                                .endedBy(liveRoom.getUserId())
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

    @Override
    public Mono<LiveRoomGridViewResponseDto_1> getLiveRoomById_1(GridViewRequestDto gridViewRequestDto) {

        return port.getLiveRoomById(gridViewRequestDto.getKeycloakId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, ExceptionMessages.NO_LIVE_ROOM_FOUND_WITH_ID.getValue())))
                .map(liveRoom -> LiveRoomGridViewResponseDto_1
                        .builder()
                        .message("Live room by id is fetched successfully")
                        .data(this.buildLiveRoomResponse(liveRoom))
                        .count(1)
                        .error(false)
                        .build()
                );
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
        liveRoomResponse.setCreatedOn(liveRoom.getCreatedOn());

        return liveRoomResponse;
    }

    private LiveRoomResponse buildLiveRoomCreateResponse(LiveRoom liveRoom) {
        LiveRoomResponse liveRoomResponse = modelMapper.map(liveRoom, LiveRoomResponse.class);
        liveRoomResponse.setHostMaxId(liveRoom.getHostMaxId());
        liveRoomResponse.setAnnouncement(liveRoom.getAnnouncement());
        liveRoomResponse.setCreatedOn(liveRoom.getCreatedOn());

        return liveRoomResponse;
    }

    private List<LiveRoomResponse> buildLiveRoomResponse(List<LiveRoom> liveRoomList) {
        List<LiveRoomResponse> liveRoomResponseList = new ArrayList<>();
        for (LiveRoom liveRoom : liveRoomList) {
//            Map<String, Fan> fanMap = liveRoom.getFans();
            LiveRoomResponse liveRoomResponse = modelMapper.map(liveRoom, LiveRoomResponse.class);
            liveRoomResponse.setCreatedOn(liveRoom.getCreatedOn());
            liveRoomResponse.setHostDailyGemsValue(CommonBusiness.convertToShortName(liveRoom.getHostDailyGems()));
            liveRoomResponse.setGender(liveRoom.getHostGender());
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

        if (liveRoom.getAudioParticipants() != null && !liveRoom.getAudioParticipants().isEmpty()) {
            liveRoom.getAudioParticipants().removeIf(viewer -> viewer.getUserId().equals(user.getId()));
        }

        if (liveRoom.getJoinRequests() != null && !liveRoom.getJoinRequests().isEmpty()) {
            liveRoom.getJoinRequests().removeIf(joinRequests -> joinRequests.getUserId().equals(user.getId()));
        }

        Viewer viewer = Viewer
                .builder()
                .userId(user.getId())
                .displayName(user.getDisplayName())
                .gender(user.getGender())
                .profilePictureUrl(user.getProfileImageUrl())
                .profileFrameUrl(user.getProfileFrameUrl())
                .profileFrameId(user.getProfileFrameId())
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
                        .description(Strings.isNotNullAndNotEmpty(requestDto.getDescription()) ? requestDto.getDescription() : "Welcome to my Live")
                        .tags(requestDto.getTags())
                        .type(requestDto.getType())
                        .enableJoin(requestDto.getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())
                                ? Constants.STATUS_YES.getValue()
                                : Constants.STATUS_NO.getValue())
                        .status(Constants.STATUS_LIVE.getValue())
                        .country(host.getCountry())
                        .hostId(host.getId())
                        .userId(host.getUserId())
                        .hostMaxId(host.getMaxId())
                        .kickedOutUserIds(new ArrayList<>())
                        .viewerCount(0)
                        .hostDailyGems(dailyReceivedGems)
                        .createdOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant())
                        .hostGender(host.getGender())
                        .audioSeatNumber(requestDto.getAudioSeatNumber() != null ? requestDto.getAudioSeatNumber() : 10)
                        .build());
    }

    private Mono<LiveRoom> buildAudioLiveRoomDomain(Host host, LiveRoomRequestDto requestDto) {
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
                        .type(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())
                        .status(Constants.STATUS_LIVE.getValue())
                        .country(host.getCountry())
                        .hostId(host.getId())
                        .userId(host.getUserId())
                        .hostMaxId(host.getMaxId())
                        .kickedOutUserIds(new ArrayList<>())
                        .viewerCount(0)
                        .hostDailyGems(dailyReceivedGems)
                        .createdOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant())
                        .maxAudioParticipants(LiveRoomConfigEnums.maxAudioParticipants.getValue())
                        .audioParticipants(new ArrayList<>())
                        .hostGender(host.getGender())
                        .audioSeatNumber(requestDto.getAudioSeatNumber())
                        .audioSkinId(Strings.isNotNullAndNotEmpty(requestDto.getAudioSkinId()) ? requestDto.getAudioSkinId() : "")
                        .enableJoin(Constants.STATUS_YES.getValue())
                        .enableAutoJoin(Constants.STATUS_NO.getValue())
                        .build());
    }

    @Override
    public Mono<Earning> userEarning(String keycloakId) {
        int month = ZonedDateTime.now(ZoneOffset.UTC).getMonthValue();
        int year = ZonedDateTime.now(ZoneOffset.UTC).getYear();

        // Step 1: Fetch User by Keycloak ID
        return userUseCase.getUserByKeycloakId(keycloakId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found for Keycloak ID: " + keycloakId)))
                .flatMap(dbUser -> {
                    // Step 2: Fetch Host by User ID
                    Mono<Host> dbHostUser = hostUseCase.getHostByUserId(dbUser.getId())
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Host not found for User ID: " + dbUser.getId())));

                    // Step 3: Fetch pre-aggregated totals using findLiveRoomSummaryTotals
                    Mono<LiveRoomSummaryEntity> dbLiveRoomSummaryTotals = liveRoomSummaryService.findLiveRoomSummaryTotals(dbUser.getId(), month, year)
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("No summary found for User ID: " + dbUser.getId() + ", Month: " + month + ", Year: " + year)));

                    // Step 4: Combine data into Earning
                    return Mono.zip(dbHostUser, dbLiveRoomSummaryTotals)
                            .flatMap(tuple -> {
                                Host host = tuple.getT1();
                                LiveRoomSummaryEntity summary = tuple.getT2();
                                double totalBonus = summary.getTotalBonus();

                                log.info("Combining data for userId {}: hostType={}, totalDurations={}, totalLiveDays={}, totalBonus={}",
                                        dbUser.getId(), host.getHostType(), summary.getTotalVideoDuration(), summary.getTotalLiveDays(), totalBonus);

                                // Build Earning object based on hostType
                                Earning.EarningBuilder earningBuilder = Earning.builder()
                                        .month(String.valueOf(month))
                                        .year(year)
                                        .gems(dbUser.getGems())
                                        .hostType(host.getHostType())
                                        .gemsString(FormatUtil.convertToShortName(dbUser.getGems()));

                                if ("video".equalsIgnoreCase(host.getHostType())) {
                                    earningBuilder
                                            .duration(String.valueOf(summary.getTotalVideoDuration()))
                                            .durationString(FormatUtil.convertDurationToString(summary.getTotalVideoDuration()))
                                            .validDays(summary.getTotalLiveDays())
                                            .bonus(totalBonus)
                                            .bonusString(FormatUtil.convertToShortName(totalBonus));
                                } else if ("audio".equalsIgnoreCase(host.getHostType())) {
                                    log.info("Audio host type detected; only gems and gemsString fields will be populated for userId {}", dbUser.getId());
                                    // Other fields remain empty
                                }

                                return Mono.just(earningBuilder.build());
                            });
                })
                .doOnError(e -> log.error("Error computing user earnings for keycloakId {}: {}", keycloakId, e.getMessage()));
    }

    @Override
    public Mono<JoinCallResponseDto> updateJoinCall(JoinCallRequestUpdateDto requestDto) {
        List<String> validValue = List.of(Constants.STATUS_YES.getValue(), Constants.STATUS_NO.getValue());
        List<String> validCameraViewValue = List.of(Constants.CAMERA_VIEW_FRONT.getValue(), Constants.CAMERA_VIEW_BACK.getValue());
        List<String> validMediaType = List.of(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue(), Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue());

        if (Strings.isNullOrEmpty(requestDto.getMediaType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Media Type is required!"));
        }
        if (!validMediaType.contains(requestDto.getMediaType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid media type!"));
        }

        String mediaType = requestDto.getMediaType();

        if (mediaType.equals(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue()) && Strings.isNotNullAndNotEmpty(requestDto.getCameraOn()) && !validValue.contains(requestDto.getCameraOn())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid CameraOn value"));
        } else if (Strings.isNotNullAndNotEmpty(requestDto.getMicOn()) && !validValue.contains(requestDto.getMicOn())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid MicOn value"));
        } else if (mediaType.equals(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue())
                && Strings.isNotNullAndNotEmpty(requestDto.getCameraOn())
                && requestDto.getCameraOn().equals(Constants.STATUS_YES.getValue())
                && Strings.isNotNullAndNotEmpty(requestDto.getCameraView())
                && !validCameraViewValue.contains(requestDto.getCameraView())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid CameraView value"));
        } else if (mediaType.equals(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue()) && Strings.isNotNullAndNotEmpty(requestDto.getCameraView()) && requestDto.getCameraOn().equals(Constants.STATUS_NO.getValue())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid CameraView value"));
        }

        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not Found!")))
                .zipWith(userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not Found!"))))
                .filter(liveRoomUserTuple2 -> liveRoomUserTuple2.getT1().getStatus().equals(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not live")))
                .flatMap(tupleOfLiveRoomAndUser -> {
                    return cachePort.getLiveRoomById(requestDto.getLiveRoomId())
                            .map(LiveRoomFirebaseEntity::getJoinRequests)
                            .flatMap(joinRequests -> {
                                if (joinRequests == null || joinRequests.isEmpty()) {
                                    return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No Join Requests found for the LiveRoom"));
                                } else if (joinRequests.stream().noneMatch(joinRequest -> joinRequest.getUserId().equals(tupleOfLiveRoomAndUser.getT2().getId()))) {
                                    return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join Request not found for the User"));
                                }

                                return Mono.just(tupleOfLiveRoomAndUser);
                            });
                })
                .flatMap(liveRoomUserTuple2 -> cachePort.updateJoinedCall(liveRoomUserTuple2.getT1(), requestDto)
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom  updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage())))
                .map(liveRoom -> JoinCallResponseDto
                        .builder()
                        .message("Join call request updated successfully.")
                        .data(LiveRoomJoinRequestInfo
                                .builder()
                                .roomId(liveRoom.getId())
                                .requestId(requestDto.getRequestId())
                                .micOn(requestDto.getMicOn())
                                .cameraOn(requestDto.getCameraOn())
                                .cameraView(requestDto.getCameraView())
                                .build())
                        .count(1)
                        .error(false)
                        .build());
    }

    @Override
    public Mono<HostMicStatusResponseDto> setMicStatus(String liveRoomId, String micOn) {

        return cachePort.getLiveRoomById(liveRoomId)
                .doOnSuccess(liveRoomFirebaseEntity -> log.info("LiveRoom host : {}", liveRoomFirebaseEntity.getHost().toString()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, ExceptionMessages.NO_LIVE_ROOM_FOUND_WITH_ID.getValue())))
                .map(liveRoomFirebaseEntity -> {
                    if (StringUtil.isNullOrEmpty(micOn))
                        liveRoomFirebaseEntity.getHost().setMicOn("Yes");
                    else
                        liveRoomFirebaseEntity.getHost().setMicOn(micOn);

                    return liveRoomFirebaseEntity;
                })
                .flatMap(liveRoomFirebaseEntity -> cachePort.updateByEntity(liveRoomFirebaseEntity).thenReturn(liveRoomFirebaseEntity.getHost()))
                .map(hostSummary -> HostMicStatusResponseDto
                        .builder()
                        .message("Host Mic status is set properly")
                        .data(hostSummary)
                        .build()
                );

    }

    @Override
    public Mono<JoinCallResponseDto> autoJoinProcess(JoinCallRequestDto requestDto) {

        List<String> validMedia = List.of(Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue(), Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue());
        if (!validMedia.contains(requestDto.getMediaType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid media type!"));
        }

        if (Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue().equals(requestDto.getMediaType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Auto join is only available for Video type media."));
        }

        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found")))
                .zipWith(userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found"))))
                .flatMap(liveRoomUserTuple -> cachePort.getLiveRoomById(requestDto.getLiveRoomId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found in firebase")))
                        .map(firebaseEntity -> Tuples.of(liveRoomUserTuple.getT1(), liveRoomUserTuple.getT2(), firebaseEntity)))
                .flatMap(tuple -> {
                    LiveRoom liveRoom = tuple.getT1();
                    User user = tuple.getT2();
                    LiveRoomFirebaseEntity firebaseEntity = tuple.getT3();

                    SeatNumberDto requestedSeat = firebaseEntity.getSeatMap().get("Seat_" + requestDto.getSeatNumber());
                    return this.validateJoinRequest(liveRoom, user, requestedSeat, firebaseEntity)
                            .flatMap(liveRoomFirebaseEntity -> {
                                List<JoinRequests> existingJoinRequest = new ArrayList<>();

                                if (firebaseEntity.getJoinRequests() != null) {
                                    existingJoinRequest = firebaseEntity.getJoinRequests()
                                            .stream()
                                            .filter(jr -> jr.getUserId().equals(user.getId()))
                                            .toList();
                                }

                                SeatNumberDto seatNumberDto = new SeatNumberDto();
                                seatNumberDto.setAvailableStatus(false);
                                seatNumberDto.setUserId(user.getId());
                                log.info("existingJoinRequest: {}", existingJoinRequest);
                                if (!existingJoinRequest.isEmpty()) {
                                    log.debug("User already has a join request. Updating existing request. : {}", existingJoinRequest);
                                    seatNumberDto.setJoinReqId(existingJoinRequest.get(0).getRequestId());
                                }

                                log.info("seatNumberDto : {}", seatNumberDto);

                                return Strings.isNotNullAndNotEmpty(seatNumberDto.getJoinReqId())
                                        ? this.updateFirebaseSeatMapForExistingJoinRequest(firebaseEntity, requestDto, seatNumberDto, liveRoom)
                                        : this.buildJoinRequestAndUpdateFirebaseSeatMap(user, requestDto, seatNumberDto, liveRoom, firebaseEntity);
                            })
                            .map(liveRoomJoinRequestInfo -> Tuples.of(liveRoom.getId(), user.getMaxId(), liveRoomJoinRequestInfo));
                })
                .flatMap(tuple3 -> agoraService
                        .generateAgoraToken(tuple3.getT1(), tuple3.getT2(), AgoraTokenTypeEnum.ROLE_PUBLISHER.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue())
                        .map(token -> {
                            tuple3.getT3().setAgoraToken(token);
                            tuple3.getT3().setRole(AgoraTokenTypeEnum.ROLE_PUBLISHER.getValue());
                            return tuple3.getT3();
                        }))
                .doOnError(throwable -> log.error("Error occurred while processing auto join call request: {}", throwable.getMessage()))
                .map(response -> JoinCallResponseDto.builder()
                        .message("Auto Start Join call request processed successfully.")
                        .data(response)
                        .count(1)
                        .error(false)
                        .build());
    }

    private Mono<LiveRoomJoinRequestInfo> updateFirebaseSeatMapForExistingJoinRequest(LiveRoomFirebaseEntity firebaseEntity, JoinCallRequestDto requestDto, SeatNumberDto seatNumberDto, LiveRoom liveRoom) {
        return Mono.just(firebaseEntity)
                .flatMap(liveRoomFirebaseEntity -> cachePort.updateAudioSeatMap(liveRoomFirebaseEntity.getId(), requestDto.getSeatNumber(), seatNumberDto)
                        .map(liveRoomFirebaseEntity1 -> LiveRoomJoinRequestInfo.builder()
                                .roomId(requestDto.getLiveRoomId())
                                .status(liveRoom.getStatus())
                                .requestId(seatNumberDto.getJoinReqId())
                                .micOn(requestDto.getMicOn())
                                .cameraOn(requestDto.getCameraOn())
                                .cameraView(requestDto.getCameraView())
                                .build()));
    }

    private Mono<LiveRoomJoinRequestInfo> buildJoinRequestAndUpdateFirebaseSeatMap(User user, JoinCallRequestDto requestDto, SeatNumberDto seatNumberDto, LiveRoom liveRoom, LiveRoomFirebaseEntity firebaseEntity) {
        return buildJoinRequest(user, requestDto, liveRoom)
                .flatMap(joinRequests -> {
                    joinRequests.forEach(jr -> jr.setStatus(Constants.STATUS_STARTED.getValue()));
                    liveRoom.setJoinRequests(joinRequests);
                    return port.saveLiveRoom(liveRoom)
                            .flatMap(savedRoom -> cachePort.updateForJoinRequest(savedRoom)
                                    .thenReturn(Tuples.of(savedRoom, user)));
                })
                .flatMap(tuple -> buildJoinRequestResponse(tuple.getT1(), tuple.getT2(), requestDto)
                        .map(response -> Tuples.of(response, tuple.getT1())))
                .map(Tuple2::getT1)
                .flatMap(liveRoomJoinRequestInfo -> {
                    String joinReqId = liveRoomJoinRequestInfo.getRequestId();
                    seatNumberDto.setJoinReqId(joinReqId);
                    return cachePort.updateAudioSeatMap(firebaseEntity.getId(), requestDto.getSeatNumber(), seatNumberDto)
                            .thenReturn(liveRoomJoinRequestInfo);
                });
    }

    private Mono<LiveRoomFirebaseEntity> validateJoinRequest(LiveRoom liveRoom, User user, SeatNumberDto requestedSeat, LiveRoomFirebaseEntity firebaseEntity) {
        if (liveRoom.getEnableAutoJoin() == null) {
            return Mono.error(new IllegalAccessError("Auto Join call is not enabled for the LiveRoom"));
        }

        if (liveRoom.getEnableAutoJoin().equals(Constants.STATUS_NO.getValue())) {
            return Mono.error(new IllegalAccessError("Auto Join call is not enabled for the LiveRoom"));
        }
        if (!Constants.STATUS_YES.getValue().equalsIgnoreCase(user.getActive())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not active"));
        }
        if (liveRoom.getHostId().equals(user.getId())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "The user is the host of the LiveRoom and cannot join as a participant."));
        }
        if (liveRoom.getKickedOutUserIds().stream().anyMatch(id -> id.equalsIgnoreCase(user.getId()))) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "The user has been kicked out of the LiveRoom and cannot rejoin as a participant."));
        }
        if (!Constants.STATUS_LIVE.getValue().equalsIgnoreCase(liveRoom.getStatus())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not live"));
        }
        if (liveRoom.getViewerIds() == null ||
                liveRoom.getViewerIds().stream().noneMatch(id -> id.equalsIgnoreCase(user.getId()))) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is not a viewer of the LiveRoom and cannot join as a participant."));
        }

        if (!requestedSeat.isAvailableStatus()) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Seat is not available to join"));
        }

        return Mono.just(firebaseEntity);
    }


    @Override
    public Mono<LiveRoomHeartBeatResponse> updateLivenessHeartbeat(String liveRoomId) {
        log.info("Updating liveness heartbeat for liveroom: {}", liveRoomId);
        return this.getLiveRoomById(liveRoomId)
                .doOnNext(liveRoom -> log.info("Found liveroom for heartbeat update: {}", liveRoom.getId()))
                .filter(liveRoom -> Constants.STATUS_LIVE.getValue().equalsIgnoreCase(liveRoom.getStatus()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom is not in live status")))
                .map(liveRoom -> {
                    liveRoom.setLastSeen(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
                    return liveRoom;
                })
                .flatMap(this::updateLiveRoom)
                .doOnNext(updatedRoom -> log.info("Successfully updated liveness heartbeat for liveroom: {}", updatedRoom.getId()))
                .map(liveRoom -> LiveRoomHeartBeatDto.builder()
                        .liveRoomId(liveRoom.getId())
                        .time(liveRoom.getLastSeen())
                        .build())
                .map(liveRoomHeartBeatDto -> LiveRoomHeartBeatResponse
                        .builder()
                        .message("Liveness heartbeat updated successfully.")
                        .data(liveRoomHeartBeatDto)
                        .count(1)
                        .error(false)
                        .build())
                .doOnError(error -> log.error("Error updating liveness heartbeat for liveroom {}: {}", liveRoomId, error.getMessage()));
    }

}
