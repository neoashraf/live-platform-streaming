package com.tanvir.features.giftsummary.application.port.in;

import com.tanvir.features.giftsummary.domain.GiftSummary;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import reactor.core.publisher.Mono;

public interface GiftSummaryUseCase {
    Mono<GiftSummary> saveGiftSummary(GiftSummary giftSummary);
    Mono<GiftTransaction> buildAndSaveGiftSummary(GiftTransaction giftTransaction);
    Mono<GiftSummary> getGiftSummaryByUserId(String userId);
}
