package com.tanvir.features.gifttransaction.application.port.in;

import com.tanvir.features.gifttransaction.application.port.in.dto.request.BeanTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.SendGiftRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.GetBeanTransactionsResponseDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.SendGiftResponseDto;
import reactor.core.publisher.Mono;

public interface GiftTransactionUseCase {
    Mono<SendGiftResponseDto> sendGifts(SendGiftRequestDto requestDto);
}
