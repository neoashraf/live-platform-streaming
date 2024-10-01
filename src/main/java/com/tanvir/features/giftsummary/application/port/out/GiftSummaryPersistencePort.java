package com.tanvir.features.giftsummary.application.port.out;

import com.tanvir.features.giftsummary.domain.GiftSummary;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface GiftSummaryPersistencePort {
    Mono<List<GiftSummary>> getGiftSummaryByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore);

    Mono<List<UserBeanSummary>> getHostGiftSummariesByDate(LocalDateTime createdAfter, LocalDateTime createdBefore);

    Mono<Double> getTotalGiftAmountByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore);
}
