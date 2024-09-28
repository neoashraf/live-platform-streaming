package com.tanvir.features.liveroom.application.port.in;

import com.tanvir.features.liveroom.application.port.in.dto.request.*;
import com.tanvir.features.liveroom.application.port.in.dto.response.EndStreamResponseDto;
import com.tanvir.features.liveroom.application.port.in.dto.response.LiveRoomGridViewResponseDto;
import com.tanvir.features.liveroom.application.port.in.dto.response.LiveRoomResponseDto;
import com.tanvir.features.liveroom.application.port.in.dto.response.StreamResponseDto;
import reactor.core.publisher.Mono;

public interface LiveRoomUseCase {
    Mono<LiveRoomGridViewResponseDto> createStream(LiveRoomRequestDto requestDto);
    Mono<StreamResponseDto> joinStream(LiveRoomEntryLeaveRequestDto requestDto);
    Mono<StreamResponseDto> leaveStream(LiveRoomEntryLeaveRequestDto requestDto);
    Mono<EndStreamResponseDto> endStream(String liveRoomId, String userId);
    Mono<LiveRoomResponseDto> kickOutUser(KickOutUserRequestDto requestDto);
    Mono<LiveRoomResponseDto> getLiveRoomDetailViewById(String id);
    Mono<LiveRoomGridViewResponseDto> getHomepage(GridViewRequestDto requestDto);
    Mono<LiveRoomResponseDto> sendGift(SendGiftRequestDto requestDto);

}
