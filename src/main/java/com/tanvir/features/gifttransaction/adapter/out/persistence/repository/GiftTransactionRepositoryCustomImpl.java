package com.tanvir.features.gifttransaction.adapter.out.persistence.repository;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import com.tanvir.features.gifttransaction.domain.LiveRoomTotalBeans;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GiftTransactionRepositoryCustomImpl implements GiftTransactionRepositoryCustom {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<GiftTransactionEntity> findAllByFilters(String senderId, String receiverId, String searchKey, Instant createdAfter, Instant createdBefore, Pageable pageable) {

        List<Criteria> criteriaList = new ArrayList<>();

        // Criteria for sender/receiver Id and UserType with 'or' relationship
        List<Criteria> senderReceiverCriteria = new ArrayList<>();

        if (senderId != null && !senderId.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("senderId").is(senderId));
        }

        if (receiverId != null && !receiverId.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("receiverId").is(receiverId));
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


        // Criteria for searchKey
        if (searchKey != null && !searchKey.isEmpty()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("senderId").regex(searchKey, "i"),
                    Criteria.where("receiverId").regex(searchKey, "i"),
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
        Query query = new Query(criteria).with(pageable).with(Sort.by(Sort.Direction.DESC, "createdOn"));

        return reactiveMongoTemplate.find(query, GiftTransactionEntity.class);
    }

    @Override
    public Mono<Long> getCountByFilters(String senderId, String receiverId, String searchKey, Instant fromDate, Instant toDate) {


        // Create a list to hold individual criteria
        List<Criteria> criteriaList = new ArrayList<>();

        // Add criteria for senderId if it's provided
        if (senderId != null && !senderId.isEmpty()) {
            criteriaList.add(Criteria.where("senderId").is(senderId));
        }

        // Add criteria for receiverId if it's provided
        if (receiverId != null && !receiverId.isEmpty()) {
            criteriaList.add(Criteria.where("receiverId").is(receiverId));
        }

        // Add criteria for searchKey if it's provided (case-insensitive regex search)
        if (searchKey != null && !searchKey.isEmpty()) {
            criteriaList.add(Criteria.where("searchKey").regex(".*" + searchKey + ".*", "i"));
        }

        // Add criteria for date range if fromDate and/or toDate are provided
        if (fromDate != null || toDate != null) {
            if (fromDate != null && toDate != null) {
                criteriaList.add(Criteria.where("createdOn").gte(fromDate).lt(toDate));
            } else if (fromDate != null) {
                criteriaList.add(Criteria.where("createdOn").gte(fromDate));
            } else {
                criteriaList.add(Criteria.where("createdOn").lt(toDate));
            }
        }

        // Combine all criteria using 'andOperator' if there are any criteria
        Criteria finalCriteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            finalCriteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // Build the query with the final criteria
        Query query = new Query(finalCriteria);

        // Perform the count operation using reactiveMongoTemplate
        return reactiveMongoTemplate.count(query, GiftTransactionEntity.class);
    }


    @Override
    public Flux<LiveRoomTotalBeans> findTotalBeansGroupedByLiveRoomId(Instant start, Instant end) {
        Aggregation aggregation = Aggregation.newAggregation(
                // Match documents that have a non-null liveRoomId and are within the date range
                Aggregation.match(Criteria.where("createdOn").gte(start).lt(end)
                        .and("liveRoomId").ne(null)), // Exclude documents with null liveRoomId
                Aggregation.group("liveRoomId")
                        .sum("beans").as("totalBeans")
                        .first("liveRoomId").as("liveRoomId"), // Ensure liveRoomId is included
                Aggregation.project("liveRoomId", "totalBeans")
        );

        return reactiveMongoTemplate.aggregate(aggregation, GiftTransactionEntity.class, LiveRoomTotalBeans.class);
    }




}
