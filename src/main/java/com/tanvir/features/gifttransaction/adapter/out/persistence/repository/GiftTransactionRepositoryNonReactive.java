package com.tanvir.features.gifttransaction.adapter.out.persistence.repository;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GiftTransactionRepositoryNonReactive extends MongoRepository<GiftTransactionEntity, String> {
}
