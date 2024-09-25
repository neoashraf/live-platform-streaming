package com.tanvir.features.host.adapter.out.persistence.repository;

import com.tanvir.features.host.adapter.out.persistence.entity.HostEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface HostRepository extends ReactiveMongoRepository<HostEntity, String> {
    Mono<HostEntity> findByUserId(String userId);
}
