package com.tanvir.features.liveroom.application.port.out;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.application.port.in.dto.request.JoinCallRequestDto;
import com.tanvir.features.liveroom.domain.LiveRoom;
import reactor.core.publisher.Mono;

public interface CachePort {
    Mono<LiveRoomFirebaseEntity> create(LiveRoomFirebaseEntity entity);
    Mono<String> delete(String id);
    Mono<LiveRoomEntity> update(LiveRoomEntity entity);
    Mono<LiveRoom> update(LiveRoom liveRoom);
    Mono<LiveRoom> updateForViewerLeave(LiveRoom liveRoom);
    Mono<LiveRoom> updateForViewerKick(LiveRoom liveRoom);
    Mono<LiveRoom> updateForComment(LiveRoom liveRoom);
    Mono<LiveRoom> updateForGift(LiveRoom liveRoom);
    Mono<LiveRoom> updateForJoinPermission(LiveRoom liveRoom);
    Mono<LiveRoom> updateForJoinRequest(LiveRoom liveRoom);
    Mono<LiveRoom> updateForProcessingJoinCall(LiveRoom liveRoom, JoinCallRequestDto joinCallRequestDto);
    Mono<LiveRoom> updateForClosingJoinedCall(LiveRoom liveRoom, JoinCallRequestDto joinCallRequestDto);

    Mono<LiveRoomFirebaseEntity> getLiveRoomById(String id);
}
