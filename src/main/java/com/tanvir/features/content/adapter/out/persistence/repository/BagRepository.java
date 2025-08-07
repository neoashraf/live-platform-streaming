package com.tanvir.features.content.adapter.out.persistence.repository;

import com.tanvir.features.content.adapter.out.persistence.entity.BagEntity;
import com.tanvir.features.content.adapter.out.persistence.entity.ContentEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface BagRepository extends ReactiveMongoRepository<BagEntity, String> {
        Mono<BagEntity> findByContentIdAndUserId(String contentId, String userId);
}
