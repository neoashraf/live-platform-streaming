package com.tanvir.features.gift.application.service;

import com.tanvir.features.gift.application.port.in.GiftUseCase;
import com.tanvir.features.gift.application.port.out.GiftPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Service
@Slf4j
public class GiftService implements GiftUseCase {

    private final GiftPersistencePort port;

    public GiftService(GiftPersistencePort port) {
        this.port = port;
    }

    @Override
    public Mono<Double> getGiftCostByGiftId(String giftId) {
        return port.getGiftCostByGiftId(giftId);
    }
}
