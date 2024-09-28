package com.tanvir.features.gift.adapter.out.persistence.repository;

import com.tanvir.features.gift.adapter.out.persistence.entity.GiftEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GiftRepository extends ReactiveMongoRepository<GiftEntity, String> {
    Flux<GiftEntity> findAllBy(Pageable pageable);
}
