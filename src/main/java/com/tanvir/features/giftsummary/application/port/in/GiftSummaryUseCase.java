package com.tanvir.features.giftsummary.application.port.in;

import com.tanvir.features.giftsummary.domain.GiftSummary;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface GiftSummaryUseCase {
    Mono<GiftSummary> saveGiftSummary(GiftSummary giftSummary);
    Mono<GiftTransaction> processGiftSummary(GiftTransaction giftTransaction);
    Mono<List<GiftSummary>> getGiftSummaryByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore);
    Mono<List<UserBeanSummary>> getHostGiftSummariesByDate(LocalDateTime createdAfter, LocalDateTime createdBefore, Integer limit);
    Mono<Double> getTotalGiftAmountByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore);
}
