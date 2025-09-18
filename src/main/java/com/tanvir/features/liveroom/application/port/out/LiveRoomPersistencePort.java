package com.tanvir.features.liveroom.application.port.out;

import com.tanvir.features.liveroom.domain.LiveRoom;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;


public interface LiveRoomPersistencePort {
    Mono<LiveRoom> saveLiveRoom(LiveRoom liveRoom);
    Mono<LiveRoom> getLiveRoomById(String id);
    Flux<LiveRoom> getActiveLiveRooms();
    Mono<LiveRoom> getActiveLiveRoomByKeyCloakId(String keycloakId);
    Flux<LiveRoom> getActiveVideoLiveRoomsByPopularityLevel(Integer popularityLevel);
    Flux<LiveRoom> getActiveAudioLiveRooms(Pageable pageable, String country);
    Flux<LiveRoom> getActiveVideoLiveRooms(Pageable pageable, String country);
    Flux<LiveRoom> getActiveVideoAndAudioLiveRooms(Pageable pageable, String country, String mediaType);
    Mono<Long> getActiveLiveRoomsCountByTypeAndCountry(String type, String country, String viewMode);

    Mono<LiveRoom> getActiveLiveRoomByHostId(String hostId);

    Mono<List<LiveRoom>> getFollowingLiveRooms(String keycloakId, Pageable pageable, String mediaType);

    Mono<Long> getFollowingLiveRoomsCount(String keycloakId, String mediaType);

    Flux<LiveRoom> findLiveRoomsForOfflineCheck(Instant cutoffTime);
    Mono<LiveRoom> save(LiveRoom liveRoom);
}
