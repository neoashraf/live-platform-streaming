package com.tanvir.features.gifttransaction.adapter.out.persistence.repository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;

public interface UserBaseRepository<T, ID> extends ReactiveCrudRepository<T, ID> {
    Mono<T> findByMaxId(String maxId);

    @Query(value = "{ 'createdOn': { $gte: ?0, $lt: ?1 } }", count = true)
    Mono<Long> countAllByCreatedOnBetween(LocalDateTime createdAfter, LocalDateTime createdBefore);

    @Query(value = "{ 'createdOn': { $gte: ?0, $lt: ?1 } }", count = true)
    Mono<Long> countAllByCreatedOnBetween(Instant createdAfter, Instant createdBefore);

}
