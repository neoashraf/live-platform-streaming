package com.tanvir.features.gifttransaction.application.port.out;

import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GiftTransactionPersistencePort {
    Mono<GiftTransaction> saveTransaction(GiftTransaction giftTransaction);
    Flux<GiftTransaction> getBeanTransactions(GiftTransactionRequestDto requestDto);
    Mono<Long> getBeanTransactionsCount(GiftTransactionRequestDto requestDto);
}
