package com.tanvir.features.giftsummary.adapter.out.persistence.repository;

import com.tanvir.features.giftsummary.adapter.out.persistence.entity.GiftSummaryEntity;
import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface GiftSummaryRepositoryCustom {
    Flux<GiftSummaryEntity> findAllByFilters(String receiverId, String agencyId, Instant fromDate, Instant toDate);

    Mono<Long> getCountByFilters(String senderOid, String receiverOid, String searchKey, Instant fromDate, Instant toDate);

    Mono<Double> getTotalBeansByUserIdAndDate(String userId, Instant createdAfter, Instant createdBefore);
}
