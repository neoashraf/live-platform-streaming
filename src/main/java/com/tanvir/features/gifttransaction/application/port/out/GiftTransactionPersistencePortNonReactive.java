package com.tanvir.features.gifttransaction.application.port.out;

import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import reactor.core.publisher.Flux;

import java.util.List;

public interface GiftTransactionPersistencePortNonReactive {
    List<GiftTransaction> getBeanTransactions(GiftTransactionRequestDto requestDto);
}
