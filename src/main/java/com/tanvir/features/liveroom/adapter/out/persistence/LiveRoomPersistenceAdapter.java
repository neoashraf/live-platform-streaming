package com.tanvir.features.liveroom.adapter.out.persistence;

import com.tanvir.core.util.enums.Constants;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.repository.LiveRoomRepository;
import com.tanvir.features.liveroom.adapter.out.persistence.repository.LiveRoomRepositoryCustom;
import com.tanvir.features.liveroom.application.port.out.LiveRoomPersistencePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.user.application.port.in.UserUseCase;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@Component
@Slf4j
public class LiveRoomPersistenceAdapter implements LiveRoomPersistencePort {

    private final LiveRoomRepository repository;
    private final ModelMapper modelMapper;
    private final LiveRoomRepositoryCustom customRepository;
    private final UserUseCase userUseCase;

    public LiveRoomPersistenceAdapter(LiveRoomRepository liveRoomRepository, ModelMapper modelMapper, LiveRoomRepositoryCustom customRepository, UserUseCase userUseCase) {
        this.repository = liveRoomRepository;
        this.modelMapper = modelMapper;
        this.customRepository = customRepository;
        this.userUseCase = userUseCase;
    }

    @Override
    public Mono<LiveRoom> saveLiveRoom(LiveRoom liveRoom) {
        LiveRoomEntity entity = modelMapper.map(liveRoom, LiveRoomEntity.class);
        return repository.save(entity)
                .doOnRequest(l -> log.info("Request received to save LiveRoom entity to DB"))
//                .doOnSuccess(liveRoomEntity -> log.info("LiveRoom entity successfully persisted into db : {}", liveRoomEntity))
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class));
    }

    @Override
    public Mono<LiveRoom> getLiveRoomById(String id) {
        return repository.getLiveRoomEntityById(id)
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class));
    }

    @Override
    public Flux<LiveRoom> getActiveLiveRooms() {
        return repository.getAllByStatus(Constants.STATUS_LIVE.getValue())
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class));
    }

    @Override
    public Mono<LiveRoom> getActiveLiveRoomByKeyCloakId(String keycloakId) {
        /*return repository.getLiveRoomEntityByStatusAndKeycloakId(Constants.STATUS_LIVE.getValue(), keycloakId)
                .doOnRequest(l -> log.info("Request received to get active live room by keycloak id : {}", keycloakId))
                .doOnSuccess(liveRoomEntity -> log.info("Got active live room by keycloak id : {}", liveRoomEntity))
                .doOnError(throwable -> log.error("Error while getting active live room by keycloak id : {}", throwable.getMessage()))
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class));*/

        return null;
    }

    @Override
    public Flux<LiveRoom> getActiveVideoLiveRoomsByPopularityLevel(Integer popularityLevel) {
        return null;
    }

    @Override
    public Flux<LiveRoom> getActiveAudioLiveRooms(Pageable pageable, String country) {
        return /*repository.getLiveRoomEntitiesByTypeAndStatusOrderByPopularityLevelDesc(Constants.LIVE_ROOM_TYPE_AUDIO.getValue(), Constants.STATUS_LIVE.getValue(), pageable)*/
        customRepository.findAllByFilters(Constants.LIVE_ROOM_TYPE_AUDIO.getValue(), Constants.STATUS_LIVE.getValue(), country, pageable)
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class))
                .doOnRequest(l -> log.info("Request received to get active audio live rooms"))
                .doOnComplete(() -> log.info("Got active audio live rooms"))
                .doOnError(throwable -> log.error("Error while getting active audio live rooms : {}", throwable.getMessage()));
    }

    @Override
    public Flux<LiveRoom> getActiveVideoLiveRooms(Pageable pageable, String country) {
        return /*repository.getLiveRoomEntitiesByTypeAndStatusOrderByPopularityLevelDesc(Constants.LIVE_ROOM_TYPE_VIDEO.getValue(), Constants.STATUS_LIVE.getValue(), pageable)*/
        customRepository.findAllByFilters(Constants.LIVE_ROOM_TYPE_VIDEO.getValue(), Constants.STATUS_LIVE.getValue(), country, pageable)
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class))
                .doOnRequest(l -> log.info("Request received to get active video live rooms"))
                .doOnComplete(() -> log.info("Got active video live rooms"))
                .doOnError(throwable -> log.error("Error while getting active video live rooms : {}", throwable.getMessage()));
    }

    @Override
    public Flux<LiveRoom> getActiveVideoAndAudioLiveRooms(Pageable pageable, String country, String mediaType) {
        return customRepository.findAllByFilters(mediaType, Constants.STATUS_LIVE.getValue(), country, pageable)
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class))
                .doOnRequest(l -> log.info("Request received to get active video and audio live rooms"))
                .doOnComplete(() -> log.info("Got active video and audio live rooms"))
                .doOnError(throwable -> log.error("Error while getting active video and audio live rooms : {}", throwable.getMessage()));
    }

    @Override
    public Mono<Long> getActiveLiveRoomsCountByTypeAndCountry(String type, String country, String viewMode) {
        return /*repository.countByTypeAndStatusAndCountry(type, Constants.STATUS_LIVE.getValue(), country)*/
        customRepository.getCountByFilters(type, Constants.STATUS_LIVE.getValue(), country)
                .doOnRequest(l -> log.info("Request received to get active live rooms count by type : {}", type))
                .doOnSuccess(count -> log.info("Got active live rooms count by type : {}", count))
                .doOnError(throwable -> log.error("Error while getting active live rooms count by type : {}", throwable.getMessage()));
    }

    @Override
    public Mono<LiveRoom> getActiveLiveRoomByHostId(String hostId) {
        return repository.getLiveRoomEntityByHostIdAndStatus(hostId, Constants.STATUS_LIVE.getValue())
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class));
    }

    @Override
    public Mono<List<LiveRoom>> getFollowingLiveRooms(String keycloakId, Pageable pageable, String mediaType) {
        return userUseCase.getUserByKeycloakId(keycloakId)
                .flatMap(user -> customRepository.findByUserIds(user.getFollowings(), pageable, mediaType));
    }

    @Override
    public Mono<Long> getFollowingLiveRoomsCount(String keycloakId, String mediaType) {
        return userUseCase.getUserByKeycloakId(keycloakId)
                .flatMap(user -> customRepository.getCountByFilters(user.getFollowings(),mediaType));
    }
}
