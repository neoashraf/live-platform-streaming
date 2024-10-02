package com.tanvir.features.liveroomactivity;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface LiveRomActivityRepository extends ReactiveMongoRepository<LiveRoomActivityEntity, String> {
    Mono<LiveRoomActivityEntity> findByUserId(String userId);
}
