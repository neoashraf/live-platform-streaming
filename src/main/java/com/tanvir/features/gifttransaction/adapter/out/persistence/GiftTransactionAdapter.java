package com.tanvir.features.gifttransaction.adapter.out.persistence;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import com.tanvir.features.gifttransaction.adapter.out.persistence.repository.GiftTransactionRepository;
import com.tanvir.features.gifttransaction.adapter.out.persistence.repository.GiftTransactionRepositoryCustom;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePort;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

@Component
@Slf4j
public class GiftTransactionAdapter implements GiftTransactionPersistencePort {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

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
    public Flux<GiftTransaction> getBeanTransactions(GiftTransactionRequestDto requestDto) {
        Instant createdAfterInstant = requestDto.getCreatedAfter().toInstant(ZoneOffset.UTC);
        Instant createdBeforeInstant = requestDto.getCreatedBefore().toInstant(ZoneOffset.UTC);

        return customRepository.findAllByFilters(
                requestDto.getSenderId(),
                requestDto.getReceiverId(), requestDto.getSearchKey(),
                createdAfterInstant, createdBeforeInstant, requestDto.getPageable())
                .doOnRequest(l -> log.info("Requesting to fetch filtered bean transactions with senderId : {}, receiverId : {}, searchKey : {}, createdAfter: {}, createdBefore : {}", requestDto.getSenderId(), requestDto.getReceiverId(), requestDto.getSearchKey(), requestDto.getCreatedAfter(), requestDto.getCreatedBefore()))
                .doOnError(throwable -> log.error("Error occurred while fetching bean transactions: {}", throwable.getMessage()))
                .map(giftTransactionEntity -> modelMapper.map(giftTransactionEntity, GiftTransaction.class));
    }

    @Override
    public Mono<Long> getBeanTransactionsCount(GiftTransactionRequestDto requestDto) {
        Instant createdAfterInstant = requestDto.getCreatedAfter().toInstant(ZoneOffset.UTC);
        Instant createdBeforeInstant = requestDto.getCreatedBefore().toInstant(ZoneOffset.UTC);
        return customRepository.getCountByFilters(
                requestDto.getSenderId(),
                requestDto.getReceiverId(), requestDto.getSearchKey(),
                createdAfterInstant, createdBeforeInstant)
                .doOnRequest(l -> log.info("Requesting to fetch count of filtered bean transactions with senderId : {}, receiverId : {}, searchKey : {}, createdAfter: {}, createdBefore : {}", requestDto.getSenderId(), requestDto.getReceiverId(), requestDto.getSearchKey(), requestDto.getCreatedAfter(), requestDto.getCreatedBefore()))
                .doOnError(throwable -> log.error("Error occurred while fetching count of bean transactions: {}", throwable.getMessage()));
    }

    @Override
    public Flux<GiftTransaction> getGiftTransactions(String liveRoomId) {
        return repository.findAllByLiveRoomId(liveRoomId)
                .map(this::mapToDto)
                .doOnNext(transaction -> log.info("Retrieved gift transaction: {}", transaction))
                .defaultIfEmpty(new GiftTransaction()) // Handle empty case
                .doOnTerminate(() -> log.info("Completed fetching gift transactions"));
    }


    private GiftTransaction mapToDto(GiftTransactionEntity entity) {
        return GiftTransaction.builder()
                .id(entity.getId())
                .senderId(entity.getSenderId())
                .receiverId(entity.getReceiverId())
                .giftId(entity.getGiftId())
                .quantity(entity.getQuantity())
                .beans(entity.getBeans())
                .liveSession(entity.getLiveSession())
                .liveRoomId(entity.getLiveRoomId())
                .transactionDateId(entity.getTransactionDateId())
                .transactionDate(entity.getTransactionDate())
                .createdOn(entity.getCreatedOn())
                .build();
    }
}
