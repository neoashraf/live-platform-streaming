package com.tanvir.features.liveroom.application.port.out;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import reactor.core.publisher.Mono;

public interface CachePort {
    Mono<LiveRoomEntity> create(LiveRoomEntity entity);
    Mono<Void> delete(String id);
    Mono<LiveRoomEntity> update(LiveRoomEntity entity);
}
