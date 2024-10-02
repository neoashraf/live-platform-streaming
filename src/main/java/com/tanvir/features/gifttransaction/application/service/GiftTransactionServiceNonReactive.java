package com.tanvir.features.gifttransaction.application.service;

import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCaseNonReactive;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.GiftTransactionResponseDto;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePortNonReactive;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GiftTransactionServiceNonReactive implements GiftTransactionUseCaseNonReactive {

    private final GiftTransactionPersistencePortNonReactive nonReactivePort;

    public GiftTransactionServiceNonReactive(GiftTransactionPersistencePortNonReactive nonReactivePort) {
        this.nonReactivePort = nonReactivePort;
    }

    @Override
    public List<GiftTransaction> getGiftTransactions(GiftTransactionRequestDto requestDto) {
        return nonReactivePort.getBeanTransactions(requestDto);
    }
}
