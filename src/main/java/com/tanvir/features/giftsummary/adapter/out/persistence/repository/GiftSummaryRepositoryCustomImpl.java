package com.tanvir.features.giftsummary.adapter.out.persistence.repository;

import com.tanvir.features.giftsummary.adapter.out.persistence.entity.GiftSummaryEntity;
import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.GiftTransactionEntity;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GiftSummaryRepositoryCustomImpl implements GiftSummaryRepositoryCustom {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<GiftSummaryEntity> findAllByFilters(String userId, String agencyId, Instant createdAfter, Instant createdBefore) {

        List<Criteria> criteriaList = new ArrayList<>();

        // Criteria for sender/receiver Id and UserType with 'or' relationship
        List<Criteria> senderReceiverCriteria = new ArrayList<>();

        if (userId != null && !userId.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("userId").is(userId));
        }

        if (agencyId != null && !agencyId.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("agencyId").is(agencyId));
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


        // Combine all criteria
        Criteria criteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // Query with pageable and sort by createdOn (descending)
        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "createdOn"));

        return reactiveMongoTemplate.find(query, GiftSummaryEntity.class);
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
    public Mono<Double> getTotalBeansByUserIdAndDate(String userId, Instant createdAfter, Instant createdBefore) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Criteria for sender/receiver Id and UserType with 'or' relationship
        List<Criteria> senderReceiverCriteria = new ArrayList<>();

        if (userId != null && !userId.isEmpty()) {
            senderReceiverCriteria.add(Criteria.where("userId").is(userId));
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

        // Combine all criteria
        Criteria criteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // Query with pageable and sort by createdOn (descending)
        Query query = new Query(criteria);

        return reactiveMongoTemplate.find(query, GiftSummaryEntity.class)
                .map(GiftSummaryEntity::getBeans)
                .reduce(0.0, Double::sum);
    }

    @Override
    public Flux<UserBeanSummary> findTopUsersByBeansInDateRangeWithDynamicPipeline(Instant startDate, Instant endDate, int limit, Integer offset, String agencyMaxId) {

        List<AggregationOperation> pipeline = new ArrayList<>();

        pipeline.add(Aggregation.match(Criteria.where("createdOn").gte(startDate).lte(endDate)));

        if (!"*".equals(agencyMaxId)) {
            System.out.println("Entered");
            pipeline.add(Aggregation.match(Criteria.where("agencyId").is(agencyMaxId)));
        }

        pipeline.add(Aggregation.group("userId").sum("beans").as("totalBeans"));

        pipeline.add(Aggregation.sort(Sort.by(Sort.Order.desc("totalBeans"))));

        pipeline.add(Aggregation.skip(offset));

        pipeline.add(Aggregation.limit(limit));

        Aggregation aggregation = Aggregation.newAggregation(pipeline);
        return reactiveMongoTemplate.aggregate(aggregation, "gift_summary", UserBeanSummary.class);
    }
}
