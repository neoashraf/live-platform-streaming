package com.tanvir.features.liveroomsummary.adapter.out.persistence.repository;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroomsummary.adapter.out.persistence.entity.LiveRoomSummaryEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface LiveRoomSummaryRepository extends ReactiveMongoRepository<LiveRoomSummaryEntity, String> {
    Flux<LiveRoomSummaryEntity> findByCreatedOnBetween(Instant start, Instant end);
}
