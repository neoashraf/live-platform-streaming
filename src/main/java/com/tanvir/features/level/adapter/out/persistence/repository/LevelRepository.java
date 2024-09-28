package com.tanvir.features.level.adapter.out.persistence.repository;

import com.tanvir.features.level.adapter.out.persistence.entity.LevelEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface LevelRepository extends ReactiveMongoRepository<LevelEntity, String> {
    Mono<LevelEntity> findByLevel(int level);
}
