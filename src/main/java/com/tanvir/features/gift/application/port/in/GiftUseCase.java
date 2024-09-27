package com.tanvir.features.gift.application.port.in;

import reactor.core.publisher.Mono;

public interface GiftUseCase {
    Mono<Double> getGiftCostByGiftId(String giftId);
}
