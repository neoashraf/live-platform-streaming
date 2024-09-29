package com.tanvir.features.gifttransaction.adapter.out.persistence.repository;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface GiftTransactionRepositoryCustom {
    public Flux<GiftTransactionEntity> findAllByFilters(
            String senderOid, String senderUserType, String receiverOid, String receiverUserType, String category,
            String transactionId, String searchKey, Instant fromDate, Instant toDate, Pageable pageable);
}
