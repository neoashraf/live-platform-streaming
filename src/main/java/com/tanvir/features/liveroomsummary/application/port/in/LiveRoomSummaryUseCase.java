package com.tanvir.features.liveroomsummary.application.port.in;

import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroomsummary.domain.LiveRoomSummary;
import com.tanvir.features.liveroomsummary.domain.dto.HostEarning;
import com.tanvir.features.liveroomsummary.domain.dto.HostEarningRequestDto;
import com.tanvir.features.liveroomsummary.domain.dto.HostEarningResponseDto;
import reactor.core.publisher.Mono;

public interface LiveRoomSummaryUseCase {
    Mono<LiveRoomSummary> processLiveRoomSummary(LiveRoom liveRoom);
    Mono<HostEarningResponseDto> getHostEarnings(HostEarningRequestDto requestDto);
}
