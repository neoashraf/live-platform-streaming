package com.tanvir.features.liveroom.application.port.in;

import com.tanvir.features.liveroom.application.port.in.dto.request.*;
import com.tanvir.features.liveroom.application.port.in.dto.response.*;
import com.tanvir.features.liveroom.domain.LiveRoom;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface LiveRoomUseCase {
    Mono<StreamResponseDto> createStream(LiveRoomRequestDto requestDto);
    Mono<StreamResponseDto> joinStream(LiveRoomViewerRequestDto requestDto);
    Mono<StreamResponseDto> leaveStream(LiveRoomViewerRequestDto requestDto);
    Mono<StreamResponseDto> endStream(String liveRoomId, String keycloakId);
    Mono<StreamResponseDto> kickOutUser(KickOutUserRequestDto requestDto);
    Mono<LiveRoomResponseDto> getLiveRoomDetailViewById(String id);


    Mono<LiveRoomGridViewResponseDto> getHomepage(GridViewRequestDto requestDto);


    Mono<LiveRoomResponseDto> sendGift(SendGiftRequestDto requestDto);
    Mono<StreamResponseDto> comment(LiveRoomViewerRequestDto requestDto);
    Mono<LiveRoom> getLiveRoomById(String id);

    Mono<LiveRoom> updateLiveRoom(LiveRoom liveRoom);
    Mono<LiveRoomJoinPermissionResponseDto> setJoinPermission(JoinPermissionRequestDTO requestDTO);
    Mono<JoinCallResponseDto> requestJoinCall(JoinCallRequestDto requestDto);
    Mono<JoinCallResponseDto> processJoinCall(JoinCallRequestDto requestDto);
    Mono<JoinCallResponseDto> startJoinCall(JoinCallRequestDto requestDto);
    Mono<JoinCallResponseDto> closeJoinedCall(JoinCallRequestDto requestDto);

    Mono<StreamResponseDto> createAudioStream(LiveRoomRequestDto requestDto);
    Mono<StreamResponseDto> joinAudioStream(LiveRoomViewerRequestDto requestDto);

    Mono<LiveRoomGridViewResponseDto> getLiveRoomById_1(GridViewRequestDto gridViewRequestDto);
}
