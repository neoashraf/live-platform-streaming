package com.tanvir.features.gifttransaction.adapter.out.persistence.repository;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.MaxUserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MaxUserRepository extends ReactiveMongoRepository<MaxUserEntity, String>, UserBaseRepository<MaxUserEntity, String> {
    Flux<MaxUserEntity> findAllByOrderByCreatedOnDesc(Pageable pageable);
    Mono<MaxUserEntity> findMaxUserEntityByMaxId(String maxId);
    Mono<MaxUserEntity> findMaxUserEntityByUserId(String userId);
}