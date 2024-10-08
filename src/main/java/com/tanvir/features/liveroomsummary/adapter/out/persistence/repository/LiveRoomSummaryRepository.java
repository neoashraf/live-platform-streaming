package com.tanvir.features.liveroomsummary.adapter.out.persistence.repository;

import com.tanvir.features.liveroomsummary.adapter.out.persistence.entity.LiveRoomSummaryEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface LiveRoomSummaryRepository extends ReactiveMongoRepository<LiveRoomSummaryEntity, String> {
}
