package com.tanvir.features.gifttransaction.adapter.out.persistence.repository;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import com.tanvir.features.gifttransaction.domain.LiveRoomTotalBeans;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface GiftTransactionRepositoryCustom {
    public Flux<GiftTransactionEntity> findAllByFilters(
            String senderOid, String receiverOid, String searchKey, Instant fromDate, Instant toDate, Pageable pageable);

    Mono<Long> getCountByFilters(String senderOid, String receiverOid, String searchKey, Instant fromDate, Instant toDate);
    Flux<LiveRoomTotalBeans> findTotalBeansGroupedByLiveRoomId(Instant start, Instant end);
}
