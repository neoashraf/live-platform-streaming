package com.tanvir.features.gifttransaction.adapter.out.persistence;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import com.tanvir.features.gifttransaction.adapter.out.persistence.repository.GiftTransactionRepository;
import com.tanvir.features.gifttransaction.adapter.out.persistence.repository.GiftTransactionRepositoryCustom;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.BeanTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePort;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;

@Component
@Slf4j
public class GiftTransactionAdapter implements GiftTransactionPersistencePort {

    private final GiftTransactionRepository repository;
    private final GiftTransactionRepositoryCustom customRepository;
    private final ModelMapper modelMapper;
    public GiftTransactionAdapter(GiftTransactionRepository repository, GiftTransactionRepositoryCustom customRepository, ModelMapper modelMapper) {
        this.repository = repository;
        this.customRepository = customRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Mono<GiftTransaction> saveTransaction(GiftTransaction giftTransaction) {
        GiftTransactionEntity entity = modelMapper.map(giftTransaction, GiftTransactionEntity.class);
        return repository
                .save(entity)
                .doOnSuccess(giftTransactionEntity -> log.info("Transaction saved successfully: {}", giftTransactionEntity))
                .doOnError(throwable -> log.error("Error occurred while saving transaction: {}", throwable.getMessage()))
                .map(giftTransactionEntity -> modelMapper.map(giftTransactionEntity, GiftTransaction.class));
    }

    @Override
    public Flux<GiftTransaction> getBeanTransactions(BeanTransactionRequestDto requestDto) {
        Instant createdAfterInstant = requestDto.getCreatedAfter().toInstant(ZoneOffset.UTC);
        Instant createdBeforeInstant = requestDto.getCreatedBefore().toInstant(ZoneOffset.UTC);
        return customRepository.findAllByFilters(
                requestDto.getSenderId(), requestDto.getSenderUserType(),
                requestDto.getReceiverId(), requestDto.getReceiverUserType(), requestDto.getCategory(), requestDto.getTransactionId(), requestDto.getSearchKey(),
                createdAfterInstant, createdBeforeInstant, requestDto.getPageable())
                .doOnRequest(l -> log.info("Requesting to fetch filtered bean transactions with senderId : {}, senderUserType : {}, receiverId : {}, receiverUserType : {}, category : {}, searchKey : {}, createdAfter: {}, createdBefore : {}", requestDto.getSenderId(), requestDto.getSenderUserType(), requestDto.getReceiverId(), requestDto.getReceiverUserType(), requestDto.getCategory(), requestDto.getSearchKey(), requestDto.getCreatedAfter(), requestDto.getCreatedBefore()))
                .doOnError(throwable -> log.error("Error occurred while fetching bean transactions: {}", throwable.getMessage()))
                .map(giftTransactionEntity -> modelMapper.map(giftTransactionEntity, GiftTransaction.class));
    }
}
