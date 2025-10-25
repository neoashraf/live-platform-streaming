package com.tanvir.features.liveroom.application.port.out;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.application.port.in.dto.request.JoinCallRequestDto;
import com.tanvir.features.liveroom.application.port.in.dto.request.JoinCallRequestUpdateDto;
import com.tanvir.features.liveroom.application.port.in.dto.request.SeatNumberDto;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.user.domain.User;
import reactor.core.publisher.Mono;

public interface CachePort {
    Mono<LiveRoomFirebaseEntity> create(LiveRoomFirebaseEntity entity);
    Mono<String> delete(String id);
    Mono<LiveRoomEntity> update(LiveRoomEntity entity);
    Mono<LiveRoomFirebaseEntity> updateByEntity(LiveRoomFirebaseEntity liveRoomFirebaseEntity);

    Mono<LiveRoomEntity> updateForCurrentLiveRoomGiftReceived(LiveRoomFirebaseEntity liveRoomFirebaseEntity, String liveRoomId);
    Mono<LiveRoom> update(LiveRoom liveRoom);
    Mono<LiveRoom> updateForViewerLeave(LiveRoom liveRoom, User user);
    Mono<LiveRoom> updateForViewerKick(LiveRoom liveRoom);
    Mono<LiveRoom> updateForComment(LiveRoom liveRoom);
    Mono<LiveRoom> updateForGift(LiveRoom liveRoom);
    Mono<LiveRoom> updateForJoinPermission(LiveRoom liveRoom);
    Mono<LiveRoom> updateForAutoJoinPermission(LiveRoom liveRoom);
    Mono<LiveRoom> updateForJoinRequest(LiveRoom liveRoom);
    Mono<LiveRoom> updateForEndStream(LiveRoom liveRoom);
    Mono<LiveRoom> updateForProcessingJoinCall(LiveRoom liveRoom, JoinCallRequestDto joinCallRequestDto);
    Mono<LiveRoom> updateForStartingJoinCall(LiveRoom liveRoom, JoinCallRequestDto joinCallRequestDto);
    Mono<LiveRoom> updateForClosingJoinedCall(LiveRoom liveRoom, JoinCallRequestDto joinCallRequestDto);
    Mono<LiveRoom> updateJoinedCall(LiveRoom liveRoom, JoinCallRequestUpdateDto joinCallRequestUpdateDto);

    Mono<LiveRoomFirebaseEntity> getLiveRoomById(String id);

    Mono<LiveRoomFirebaseEntity> updateAudioSeatMap(String liveRoomId, Integer seatNumber, SeatNumberDto seatNumberDto);

    Mono<LiveRoom> updateForCancelJoinRequest(LiveRoom liveRoom, JoinCallRequestDto requestDto);
}
