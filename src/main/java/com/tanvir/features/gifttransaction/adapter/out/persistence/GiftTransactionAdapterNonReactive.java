package com.tanvir.features.gifttransaction.adapter.out.persistence;

import com.tanvir.features.gifttransaction.adapter.out.persistence.repository.GiftTransactionRepositoryNonReactive;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePortNonReactive;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GiftTransactionAdapterNonReactive implements GiftTransactionPersistencePortNonReactive {

    private final GiftTransactionRepositoryNonReactive nonReactiveRepository;
    private final ModelMapper modelMapper;

    public GiftTransactionAdapterNonReactive(GiftTransactionRepositoryNonReactive nonReactiveRepository, ModelMapper modelMapper) {
        this.nonReactiveRepository = nonReactiveRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<GiftTransaction> getBeanTransactions(GiftTransactionRequestDto requestDto) {
        return nonReactiveRepository.findAll()
                .stream()
                .map(giftTransactionEntity -> modelMapper.map(giftTransactionEntity, GiftTransaction.class))
                .toList();
    }
}
