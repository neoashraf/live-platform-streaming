package com.tanvir.features.liveroom.application.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tanvir.core.util.enums.Constants;
import com.tanvir.core.util.enums.ExceptionMessages;
import com.tanvir.core.util.enums.MetaPropertyEnums;
import com.tanvir.core.util.enums.UserTypeEnum;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.host.application.port.in.HostUseCase;
import com.tanvir.features.host.domain.Host;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.in.dto.request.*;
import com.tanvir.features.liveroom.application.port.in.dto.response.EndStreamResponseDto;
import com.tanvir.features.liveroom.application.port.in.dto.response.LiveRoomGridViewResponseDto;
import com.tanvir.features.liveroom.application.port.in.dto.response.LiveRoomResponse;
import com.tanvir.features.liveroom.application.port.in.dto.response.LiveRoomResponseDto;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import com.tanvir.features.liveroom.application.port.out.LiveRoomPersistencePort;
import com.tanvir.features.liveroom.domain.valueobject.DailyStarProgress;
import com.tanvir.features.liveroom.domain.valueobject.Fan;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroom.domain.valueobject.HostSummary;
import com.tanvir.features.liveroom.domain.valueobject.LiveStreamInfo;
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

    public LiveRoomService(UserUseCase userUseCase, LiveRoomPersistencePort port, MetaPropertyUseCase metaPropertyUseCase, ModelMapper modelMapper, TransactionalOperator rxtx, CachePort cachePort, HostUseCase hostUseCase) {
        this.userUseCase = userUseCase;
        this.port = port;
        this.metaPropertyUseCase = metaPropertyUseCase;
        this.modelMapper = modelMapper;
        this.rxtx = rxtx;
        this.cachePort = cachePort;
        this.hostUseCase = hostUseCase;
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
                .elapsedSeconds(0)
                .build();
    }

    @Override
    public Mono<LiveRoomResponseDto> joinStream(LiveRoomEntryLeaveRequestDto liveRoomEntryLeaveRequestDto) {
        return port.getLiveRoomById(liveRoomEntryLeaveRequestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + liveRoomEntryLeaveRequestDto.getLiveRoomId())))
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_YES.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot join.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(liveRoomEntryLeaveRequestDto.getKeycloakId())
                        .map(user -> {
                            liveRoomEntryLeaveRequestDto.getFan().setUserId(user.getId());
                            return liveRoom;
                        }))
                /*.filter(liveRoom -> !liveRoom.getFans().containsKey(liveRoomEntryLeaveRequestDto.getFan().getUserId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Fan already exists in LiveRoom.")))*/
                .flatMap(liveRoom -> {
                    if (liveRoom.getKickedOutUserIds() == null || liveRoom.getKickedOutUserIds().isEmpty()) {
                        liveRoom.setKickedOutUserIds(new ArrayList<>());
                    }
                    return Mono.just(liveRoom);
                })
                .filter(liveRoom -> !liveRoom.getKickedOutUserIds().contains(liveRoomEntryLeaveRequestDto.getFan().getUserId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "User is kicked out from LiveRoom. Cannot join.")))
                .map(liveRoom -> this.updateLiveRoomForFanEntry(liveRoom, liveRoomEntryLeaveRequestDto))
                .doOnNext(liveRoom -> log.info("Updated LiveRoom with fan entry : {}", liveRoom))
                .flatMap(port::saveLiveRoom)
                .doOnNext(liveRoom -> cachePort.update(modelMapper.map(liveRoom, LiveRoomEntity.class))
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic()).subscribe())
                .map(this::buildLiveRoomResponse)
                .map(liveRoomResponse -> this.buildLiveRoomResponseDto(liveRoomResponse, "Fan Entered into LiveRoom Successfully."))
                .doOnError(throwable -> log.error("Failed to Update LiveRoom with fan Entry. Error : {}", throwable.getMessage()));

    }

    @Override
    public Mono<LiveRoomResponseDto> leaveStream(LiveRoomEntryLeaveRequestDto requestDto) {
        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + requestDto.getLiveRoomId())))
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_YES.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot leave.")))
                .flatMap(liveRoom -> userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                        .map(user -> {
                            requestDto.setFan(Fan.builder().userId(user.getId()).build());
                            return liveRoom;
                        }))
                /*.filter(liveRoom -> liveRoom.getFans().containsKey(requestDto.getFan().getUserId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Fan doesn't exist in LiveRoom.")))*/
                .map(liveRoom -> this.updateLiveRoomForFanLeave(liveRoom, requestDto))
                .doOnNext(liveRoom -> log.info("Updated LiveRoom with fan leave : {}", liveRoom))
                .flatMap(port::saveLiveRoom)
                .doOnNext(liveRoom -> cachePort.update(modelMapper.map(liveRoom, LiveRoomEntity.class))
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic()).subscribe())
                .map(this::buildLiveRoomResponse)
                .map(liveRoomResponse -> this.buildLiveRoomResponseDto(liveRoomResponse, "Fan Left LiveRoom Successfully."))
                .doOnError(throwable -> log.error("Failed to Update LiveRoom with fan Leave. Error : {}", throwable.getMessage()));
    }

    @Override
    public Mono<EndStreamResponseDto> endStream(String liveRoomId, String keycloakId) {
        return port.getLiveRoomById(liveRoomId)
                .doOnNext(liveRoom -> log.info("LiveRoom received : {}", liveRoom))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + liveRoomId)))
                .filter(liveRoom -> liveRoom.getStatus().equalsIgnoreCase(Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Stream is not live. Cannot End.")))
               /* .filter(liveRoom -> liveRoom.getKeycloakId().equalsIgnoreCase(keycloakId))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "LiveRoom cannot be deleted by this user.")))*/
                .map(liveRoom -> {
                    liveRoom.setStatus(Constants.STATUS_NO.getValue());
                    liveRoom.setEndedOn(LocalDateTime.now());
                    return liveRoom;
                })
                .flatMap(port::saveLiveRoom)
                .flatMap(liveRoom ->
                        cachePort.delete(liveRoomId)
                        .doOnSuccess(liveRoomEntity -> log.info("LiveRoom deleted from firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while deleting LiveRoom from Firebase : {}", throwable.getMessage()))
                        .thenReturn(liveRoom))
                .flatMap(liveRoom -> this.buildEndStreamResponseDto(liveRoom));
    }

    @Override
    public Mono<LiveRoomResponseDto> kickOutUser(KickOutUserRequestDto requestDto) {
        return port.getLiveRoomById(requestDto.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "No LiveRoom found with Id : " + requestDto.getLiveRoomId())))
                /*.filter(liveRoom -> liveRoom.getKeycloakId().equalsIgnoreCase(requestDto.getKeycloakId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Fan cannot be kicked-out by this user.")))
                .filter(liveRoom -> liveRoom.getFans().containsKey(requestDto.getUserId()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Fan doesn't exist in LiveRoom. Cannot kick out.")))*/
                .map(liveRoom -> {
                   /* liveRoom.getFans().remove(requestDto.getUserId());
                    liveRoom.setFansCount(liveRoom.getFans().size());*/
                    if (liveRoom.getKickedOutUserIds() == null || liveRoom.getKickedOutUserIds().isEmpty()) {
                        liveRoom.setKickedOutUserIds(List.of(requestDto.getUserId()));
                    } else {
                        List<String> updatedKickedOutUsers = new ArrayList<>(liveRoom.getKickedOutUserIds());
                        updatedKickedOutUsers.add(requestDto.getUserId());
                        liveRoom.setKickedOutUserIds(updatedKickedOutUsers);
                    }

                    return liveRoom;
                })
                .flatMap(port::saveLiveRoom)
                .doOnNext(liveRoom -> cachePort.update(modelMapper.map(liveRoom, LiveRoomEntity.class))
                        .doOnNext(liveRoomEntity -> log.info("LiveRoom updated into firebase successfully"))
                        .doOnError(throwable -> log.error("Error Happened while updating LiveRoom into Firebase : {}", throwable.getMessage()))
                        .subscribeOn(Schedulers.boundedElastic()).subscribe())
                .map(this::buildLiveRoomResponse)
                .map(liveRoomResponse -> this.buildLiveRoomResponseDto(liveRoomResponse, "User Kicked Out Successfully."));
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


    private Mono<EndStreamResponseDto> buildEndStreamResponseDto(LiveRoom liveRoom) {
        /*EndStreamResponseDto endStreamResponseDto = new EndStreamResponseDto();
        return userUseCase.getUserById(liveRoom.getUserId())
                .map(user -> this.buildUserInfo(user, endStreamResponseDto))
                .map(responseDto -> this.buildLiveStreamInfo(liveRoom, responseDto));*/
        return Mono.just(EndStreamResponseDto
                .builder()
                .userMessage("LiveStream Ended Successfully")
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


    private LiveRoom updateLiveRoomForFanEntry(LiveRoom liveRoom, LiveRoomEntryLeaveRequestDto requestDto) {
//        Map<String, Fan> fans = liveRoom.getFans();
        Fan newFan = requestDto.getFan();
        newFan.setEntryTime(LocalDateTime.now());

       /* fans.put(newFan.getUserId(), newFan);
        liveRoom.setFans(fans);
        liveRoom.setFansCount(fans.size());*/
        return liveRoom;
    }


    private LiveRoom updateLiveRoomForFanLeave(LiveRoom liveRoom, LiveRoomEntryLeaveRequestDto requestDto) {
        /*Map<String, Fan> fans = liveRoom.getFans();
        fans.remove(requestDto.getFan().getUserId());
        liveRoom.setFans(fans);
        liveRoom.setFansCount(fans.size());*/
        return liveRoom;
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
