package com.tanvir.features.liveroom.adapter.out.persistence.repository;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface LiveRoomRepository extends ReactiveMongoRepository<LiveRoomEntity, String> {
    Mono<LiveRoomEntity> getLiveRoomEntityById(String id);
    Flux<LiveRoomEntity> getAllByStatus(String status);
    Mono<LiveRoomEntity> getLiveRoomEntityByStatusAndKeycloakId(String status, String keycloakId);

    @Query(value = "{ 'type': ?0, 'status': ?1, '$or': [ { 'country': ?2 }, { 'country': { '$exists': false } }, { 'country': '' } ] }", count = true)
    Mono<Long> countByTypeAndStatusAndCountry(String type, String status, String country);
    Flux<LiveRoomEntity> getLiveRoomEntitiesByTypeAndStatusOrderByPopularityLevelDesc(String type, String status, Pageable pageable);
}
