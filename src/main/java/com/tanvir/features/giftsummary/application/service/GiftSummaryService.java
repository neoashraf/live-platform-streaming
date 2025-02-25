package com.tanvir.features.giftsummary.application.service;
import com.tanvir.features.giftsummary.adapter.out.persistence.entity.GiftSummaryEntity;
import com.tanvir.features.giftsummary.application.port.in.GiftSummaryUseCase;
import com.tanvir.features.giftsummary.application.port.out.GiftSummaryPersistencePort;
import com.tanvir.features.giftsummary.domain.GiftSummary;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
public class GiftSummaryService implements GiftSummaryUseCase {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private GiftSummaryPersistencePort port;


    @Override
    public Mono<GiftSummary> saveGiftSummary(GiftSummary giftSummary) {
        return null;
    }

    @Override
    public Mono<GiftTransaction> processGiftSummary(GiftTransaction transaction) {
// Get the receiver's userId and the transactionDate
        String receiverId = transaction.getReceiverId();
        String transactionDateStr = transaction.getTransactionDate();

        // Parse the transactionDate string to LocalDate
        LocalDate transactionDate = transactionDateStr != null
                ? LocalDate.parse(transactionDateStr)
                 : LocalDate.now(ZoneOffset.UTC);

        // Query for existing GiftSummaryEntity by userId and transactionDate
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(receiverId)
                .and("transactionDate").is(transactionDate.toString()));

        // Find the existing summary and update or create a new one reactively
        return reactiveMongoTemplate.findOne(query, GiftSummaryEntity.class)
                .flatMap(summary -> {
                    // Update existing fields
                    List<String> existingTxnIds = new ArrayList<>(summary.getGiftTransactionIds());
                    existingTxnIds.add(transaction.getId());
                    summary.setBeans(summary.getBeans() + transaction.getBeans());
                    summary.setGiftTransactionIds(existingTxnIds);
                    summary.setTransactionCount(summary.getTransactionCount() + 1);

                    // Update sender amount map
                    Map<String, Double> senderAmountMap = summary.getSenderAmountMap();
                    senderAmountMap.put(transaction.getSenderId(),
                            senderAmountMap.getOrDefault(transaction.getSenderId(), 0.0) + transaction.getBeans());

                    summary.setSenderAmountMap(senderAmountMap);
                    summary.setUpdatedOn(transaction.getCreatedOn());
                    summary.setId(summary.getId());

                    // Save the updated summary back to the database
                    return reactiveMongoTemplate.save(summary)
                            .then(Mono.just(transaction)); // Return the transaction wrapped in Mono
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Create new GiftSummaryEntity if it does not exist
                    GiftSummaryEntity newSummary = new GiftSummaryEntity();
                    newSummary.setGiftTransactionIds(Collections.singletonList(transaction.getId()));
                    newSummary.setId(UUID.randomUUID().toString());
                    newSummary.setUserId(receiverId);
                    newSummary.setAgencyId(transaction.getAgencyId());
                    newSummary.setTransactionDate(transactionDate.toString());
                    newSummary.setBeans(transaction.getBeans());
                    newSummary.setTransactionCount(1);
                    newSummary.setSenderAmountMap(new HashMap<>());

                    // Update sender amount map for the new summary
                    Map<String, Double> senderAmountMap = newSummary.getSenderAmountMap();
                    senderAmountMap.put(transaction.getSenderId(),
                            senderAmountMap.getOrDefault(transaction.getSenderId(), 0.0) + transaction.getBeans());

                    newSummary.setSenderAmountMap(senderAmountMap);
                    newSummary.setCreatedOn(transaction.getCreatedOn());

                    // Save the new summary back to the database
                    return reactiveMongoTemplate.save(newSummary)
                            .then(Mono.just(transaction)); // Return the transaction wrapped in Mono
                }))
                .doOnError(throwable -> log.error("Error while updating gift summary"));
    }

    @Override
    public Mono<List<GiftSummary>> getGiftSummaryByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore) {
        return port.getGiftSummaryByUserIdAndDate(userId, createdAfter, createdBefore);
    }

    @Override
    public Mono<List<UserBeanSummary>> getHostGiftSummariesByDate(LocalDateTime createdAfter, LocalDateTime createdBefore, Integer limit, Integer offset, String agencyMaxId) {
        return port.getHostGiftSummariesByDate(createdAfter, createdBefore, limit,offset,agencyMaxId);
    }

    @Override
    public Mono<List<UserBeanSummary>> getAgencyGiftSummariesByDate(LocalDateTime createdAfter, LocalDateTime createdBefore, Integer limit) {
        return port.getAgencyGiftSummariesByDate(createdAfter, createdBefore, limit)
                .doOnNext(userBeanSummaries -> log.info("agencyList : {}", userBeanSummaries));
    }

    @Override
    public Mono<Double> getTotalGiftAmountByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore) {
        return port.getTotalGiftAmountByUserIdAndDate(userId, createdAfter, createdBefore);
    }
}
