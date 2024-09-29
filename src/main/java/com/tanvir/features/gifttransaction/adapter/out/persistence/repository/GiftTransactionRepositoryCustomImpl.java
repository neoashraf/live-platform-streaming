package com.tanvir.features.gifttransaction.adapter.out.persistence.repository;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GiftTransactionRepositoryCustomImpl implements GiftTransactionRepositoryCustom {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<GiftTransactionEntity> findAllByFilters(
            String senderId, String senderUserType, String receiverId, String receiverUserType, String category, String transactionId,
            String searchKey, Instant createdAfter, Instant createdBefore, Pageable pageable) {

        List<Criteria> criteriaList = new ArrayList<>();

        // Criteria for sender/receiver Id and UserType with 'or' relationship
        List<Criteria> senderReceiverCriteria = new ArrayList<>();

        if (senderId != null && !senderId.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("senderId").is(senderId));
        }
        if (senderUserType != null && !senderUserType.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("senderUserType").is(senderUserType));
        }
        if (receiverId != null && !receiverId.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("receiverId").is(receiverId));
        }
        if (receiverUserType != null && !receiverUserType.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("receiverUserType").is(receiverUserType));
        }

        // Add sender/receiver criteria only if we have any valid condition
        if (!senderReceiverCriteria.isEmpty()) {
            criteriaList.add(new Criteria().orOperator(senderReceiverCriteria.toArray(new Criteria[0])));
        }

        // Criteria for fromDate and toDate
        if (createdAfter != null || createdBefore != null) {
            if (createdAfter != null && createdBefore != null) {
                criteriaList.add(Criteria.where("createdOn").gte(createdAfter).lt(createdBefore));
            } else if (createdAfter != null) {
                criteriaList.add(Criteria.where("createdOn").gte(createdAfter));
            } else {
                criteriaList.add(Criteria.where("createdOn").lt(createdBefore));
            }
        }

        // Criteria for transactionId
        if (transactionId != null && !transactionId.isEmpty()) {
            criteriaList.add(Criteria.where("transactionId").is(transactionId));
        }

        // Criteria for category
        if (category != null && !category.isEmpty()) {
            criteriaList.add(Criteria.where("category").is(category));
        }

        // Criteria for searchKey
        if (searchKey != null && !searchKey.isEmpty()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("senderUserType").regex(searchKey, "i"),
                    Criteria.where("receiverUserType").regex(searchKey, "i"),
                    Criteria.where("receiverId").is(searchKey),
                    Criteria.where("senderId").is(searchKey)
            );
            criteriaList.add(searchCriteria);
        }

        // Combine all criteria
        Criteria criteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // Query with pageable and sort by createdOn (descending)
        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "createdOn"));

        return reactiveMongoTemplate.find(query, GiftTransactionEntity.class);
    }
}
