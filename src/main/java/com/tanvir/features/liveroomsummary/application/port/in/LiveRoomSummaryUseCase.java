package com.tanvir.features.liveroomsummary.application.port.in;

import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroomsummary.domain.LiveRoomSummary;
import reactor.core.publisher.Mono;

public interface LiveRoomSummaryUseCase {
    Mono<LiveRoomSummary> processLiveRoomSummary(LiveRoom liveRoom);
}
