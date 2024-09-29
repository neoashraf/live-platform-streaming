package com.tanvir.features.gifttransaction.application.port.in;

import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.SendGiftRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.GiftTransactionResponseDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.SendGiftResponseDto;
import reactor.core.publisher.Mono;

public interface GiftTransactionUseCase {
    Mono<SendGiftResponseDto> sendGifts(SendGiftRequestDto requestDto);
    Mono<GiftTransactionResponseDto> getGiftTransactions(GiftTransactionRequestDto requestDto);
}
