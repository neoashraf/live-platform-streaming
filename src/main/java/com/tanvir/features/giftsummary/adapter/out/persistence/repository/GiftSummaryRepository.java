package com.tanvir.features.giftsummary.adapter.out.persistence.repository;

import com.tanvir.features.giftsummary.adapter.out.persistence.entity.GiftSummaryEntity;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface GiftSummaryRepository extends ReactiveMongoRepository<GiftSummaryEntity, String> {

    //, 'agencyId': ?4
    @Aggregation(pipeline = {
            "{ $match: { 'createdOn': { $gte: ?0, $lte: ?1 } } }", // Match documents within the date range
            "{ $group: { _id: '$userId', totalBeans: { $sum: '$beans' } } }", // Group by userId and sum beans
            "{ $sort: { 'totalBeans': -1 } }", // Sort by totalBeans in descending order
            "{ $skip: ?3 }", // Skip the number of documents based on calculated skip value
            "{ $limit: ?2 }" // Limit to the top 'limit' users
    })
    Flux<UserBeanSummary> findTopUsersByBeansInDateRange(Instant startDate, Instant endDate, int limit, Integer offset, String agencyMaxId);

    @Aggregation(pipeline = {
            "{ $match: { 'createdOn': { $gte: ?0, $lte: ?1 } } }", // Match documents within the date range
            "{ $group: { _id: '$agencyId', totalBeans: { $sum: '$beans' } } }", // Group by userId and sum beans
            "{ $sort: { 'totalBeans': -1 } }", // Sort by totalBeans in descending order
            "{ $limit: ?2 }" // Limit to the top n users
    })
    Flux<UserBeanSummary> findTopAgenciesByBeansInDateRange(Instant startDate, Instant endDate, int limit);

}
