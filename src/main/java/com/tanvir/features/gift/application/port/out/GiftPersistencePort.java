package com.tanvir.features.gift.application.port.out;

import reactor.core.publisher.Mono;

public interface GiftPersistencePort {
    Mono<Double> getGiftCostByGiftId(String id);
}
