package com.tanvir.features.gifttransaction.application.port.in;

import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.GiftTransactionResponseDto;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;

import java.util.List;

public interface GiftTransactionUseCaseNonReactive {
    List<GiftTransaction> getGiftTransactions(GiftTransactionRequestDto requestDto);

}
