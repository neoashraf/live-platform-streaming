package com.tanvir.features.gift.application.port.out;

import com.tanvir.features.gift.application.port.in.dto.responseDto.GiftResponseDto;
import com.tanvir.features.gift.domain.Gift;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GiftPersistencePort {
    Mono<Double> getGiftCostByGiftId(String id);

    Flux<Gift> getGifts(Pageable pageable);
}
