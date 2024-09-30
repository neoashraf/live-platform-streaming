package com.tanvir.features.giftsummary.adapter.out.persistence.repository;

import com.tanvir.features.giftsummary.adapter.out.persistence.entity.GiftSummaryEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface GiftSummaryRepository extends ReactiveMongoRepository<GiftSummaryEntity, String> {
}
