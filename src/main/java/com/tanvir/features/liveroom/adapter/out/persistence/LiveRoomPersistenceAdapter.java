package com.tanvir.features.liveroom.adapter.out.persistence;

import com.tanvir.core.util.enums.Constants;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.repository.LiveRoomRepository;
import com.tanvir.features.liveroom.application.port.out.LiveRoomPersistencePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LiveRoomPersistenceAdapter implements LiveRoomPersistencePort {

    private final LiveRoomRepository repository;
    private final ModelMapper modelMapper;

    public LiveRoomPersistenceAdapter(LiveRoomRepository liveRoomRepository, ModelMapper modelMapper) {
        this.repository = liveRoomRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Mono<LiveRoom> saveLiveRoom(LiveRoom liveRoom) {
        LiveRoomEntity entity = modelMapper.map(liveRoom, LiveRoomEntity.class);
        return repository.save(entity)
                .doOnRequest(l -> log.info("Request received to save LiveRoom entity to DB"))
                .doOnSuccess(liveRoomEntity -> log.info("LiveRoom entity successfully persisted into db : {}", liveRoomEntity))
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class));
    }

    @Override
    public Mono<LiveRoom> getLiveRoomById(String id) {
        return repository.getLiveRoomEntityById(id)
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class));
    }

    @Override
    public Flux<LiveRoom> getActiveLiveRooms() {
        return repository.getAllByIsLive(Constants.STATUS_YES.getValue())
                .map(liveRoomEntity -> modelMapper.map(liveRoomEntity, LiveRoom.class));
    }

}
