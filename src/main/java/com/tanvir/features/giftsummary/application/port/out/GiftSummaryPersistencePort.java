package com.tanvir.features.giftsummary.application.port.out;

import com.tanvir.features.giftsummary.domain.GiftSummary;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface GiftSummaryPersistencePort {
    Mono<List<GiftSummary>> getGiftSummaryByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore);
}
