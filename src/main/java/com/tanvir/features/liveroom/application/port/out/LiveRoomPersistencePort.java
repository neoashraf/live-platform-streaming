package com.tanvir.features.liveroom.application.port.out;

import com.tanvir.features.liveroom.domain.LiveRoom;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LiveRoomPersistencePort {
    Mono<LiveRoom> saveLiveRoom(LiveRoom liveRoom);
    Mono<LiveRoom> getLiveRoomById(String id);
    Flux<LiveRoom> getActiveLiveRooms();
    Mono<LiveRoom> getActiveLiveRoomByKeyCloakId(String keycloakId);
}
