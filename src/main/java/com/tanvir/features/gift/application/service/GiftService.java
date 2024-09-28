package com.tanvir.features.gift.application.service;

import com.tanvir.features.gift.application.port.in.GiftUseCase;
import com.tanvir.features.gift.application.port.in.dto.requestDto.GiftRequestDto;
import com.tanvir.features.gift.application.port.in.dto.responseDto.GiftResponseDto;
import com.tanvir.features.gift.application.port.out.GiftPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Mono<GiftResponseDto> getGifts(Integer offSet, Integer limit) {
        Pageable pageable = PageRequest.of(offSet, limit);
        return port.getGifts(pageable)
                .collectList()
                .map(gifts -> GiftResponseDto.builder()
                        .data(gifts)
                        .count(gifts.size())
                        .message("Gifts fetched successfully")
                        .error(false)
                        .build())
                .doOnSuccess(giftResponseDto -> log.info("Gifts fetched successfully"))
                .doOnError(throwable -> log.error("Error while fetching gifts"));
    }
}
