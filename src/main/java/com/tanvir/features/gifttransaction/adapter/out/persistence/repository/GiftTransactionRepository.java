package com.tanvir.features.gifttransaction.adapter.out.persistence.repository;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GiftTransactionRepository extends ReactiveMongoRepository<GiftTransactionEntity, String> {

    Flux<GiftTransactionEntity> findAllByLiveRoomId(String liveRoomId);
}
