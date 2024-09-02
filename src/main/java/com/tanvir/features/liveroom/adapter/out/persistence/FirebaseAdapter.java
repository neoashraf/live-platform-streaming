package com.tanvir.features.liveroom.adapter.out.persistence;

import com.tanvir.core.util.DateTimeUtil;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseRepository;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class FirebaseAdapter implements CachePort {
    private final LiveRoomFirebaseRepository firebaseRepository;
    private final ModelMapper modelMapper;
    private final DateTimeUtil dateTimeUtil;

    public FirebaseAdapter(LiveRoomFirebaseRepository firebaseRepository, ModelMapper modelMapper, DateTimeUtil dateTimeUtil) {
        this.firebaseRepository = firebaseRepository;
        this.modelMapper = modelMapper;
        this.dateTimeUtil = dateTimeUtil;
    }

    @Override
    public Mono<LiveRoomEntity> create(LiveRoomEntity entity) {
        return firebaseRepository.create(modelMapper.map(entity, LiveRoomFirebaseEntity.class), entity.getId())
                .map(firebaseReturnedEntity -> modelMapper.map(firebaseReturnedEntity, LiveRoomEntity.class));
    }

    @Override
    public Mono<Void> delete(String id) {
        return firebaseRepository.delete(id);
    }

    @Override
    public Mono<LiveRoomEntity> update(LiveRoomEntity entity) {
        entity.getFans().values().forEach(fan -> fan.setEntryTime(null));
        return firebaseRepository.update(modelMapper.map(entity, LiveRoomFirebaseEntity.class), entity.getId())
                .map(firebaseReturnedEntity -> modelMapper.map(firebaseReturnedEntity, LiveRoomEntity.class));
    }
}
