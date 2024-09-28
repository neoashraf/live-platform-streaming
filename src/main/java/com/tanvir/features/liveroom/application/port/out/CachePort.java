package com.tanvir.features.liveroom.application.port.out;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.domain.LiveRoom;
import reactor.core.publisher.Mono;

public interface CachePort {
    Mono<LiveRoomFirebaseEntity> create(LiveRoomFirebaseEntity entity);
    Mono<String> delete(String id);
    Mono<LiveRoomEntity> update(LiveRoomEntity entity);
    Mono<LiveRoom> update(LiveRoom liveRoom);
    Mono<LiveRoom> updateForViewerLeave(LiveRoom liveRoom);
}
