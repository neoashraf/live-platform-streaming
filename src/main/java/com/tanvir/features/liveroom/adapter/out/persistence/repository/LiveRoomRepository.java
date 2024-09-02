package com.tanvir.features.liveroom.adapter.out.persistence.repository;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LiveRoomRepository extends ReactiveMongoRepository<LiveRoomEntity, String> {
    Mono<LiveRoomEntity> getLiveRoomEntityById(String id);
    Flux<LiveRoomEntity> getAllByIsLive(String isLive);
}
