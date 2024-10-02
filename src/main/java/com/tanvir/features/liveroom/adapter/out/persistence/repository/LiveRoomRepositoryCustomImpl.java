package com.tanvir.features.liveroom.adapter.out.persistence.repository;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class LiveRoomRepositoryCustomImpl implements LiveRoomRepositoryCustom {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<LiveRoomEntity> findAllByFilters(String type, String status, String country, Pageable pageable) {

        Criteria criteria = new Criteria();

        if (type != null && !type.isEmpty()) {
            criteria.and("type").is(type);
        }
        if (status != null && !status.isEmpty()) {
            criteria.and("status").is(status);
        }
        if (country != null && !country.isEmpty()) {
            criteria.and("country").is(country);
        }

        Query query = new Query(criteria).with(pageable).with(Sort.by(Sort.Direction.DESC, "hostDailyGems"));

        return reactiveMongoTemplate.find(query, LiveRoomEntity.class);
    }

    @Override
    public Mono<Long> getCountByFilters(String type, String status, String country) {

        Criteria criteria = new Criteria();

        if (type != null && !type.isEmpty()) {
            criteria.and("type").is(type);
        }
        if (status != null && !status.isEmpty()) {
            criteria.and("status").is(status);
        }
        if (country != null && !country.isEmpty()) {
            criteria.and("country").is(country);
        }

        Query query = new Query(criteria);

        return reactiveMongoTemplate.count(query, LiveRoomEntity.class);
    }
}
