package com.tanvir.features.gift.application.port.in;

import com.tanvir.features.gift.adapter.out.persistence.repository.GiftRepository;
import com.tanvir.features.gift.application.port.in.dto.requestDto.GiftRequestDto;
import com.tanvir.features.gift.application.port.in.dto.responseDto.GiftResponseDto;
import reactor.core.publisher.Mono;

public interface GiftUseCase {
    Mono<Double> getGiftCostByGiftId(String giftId);
    Mono<GiftResponseDto> getGifts(Integer offSet , Integer limit);
}
