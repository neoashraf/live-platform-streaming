package com.tanvir.features.liveroom.adapter.out.persistence.repository;

import com.tanvir.core.util.enums.Constants;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.domain.LiveRoom;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.testng.util.Strings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Repository
public class LiveRoomRepositoryCustomImpl implements LiveRoomRepositoryCustom {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;
    private final ModelMapper modelMapper;

    public LiveRoomRepositoryCustomImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public Flux<LiveRoomEntity> findAllByFilters(String type, String status, String country, Pageable pageable) {

        Criteria criteria = new Criteria();

        if (Strings.isNotNullAndNotEmpty(type) && !Constants.LIVE_ROOM_TYPE_ALL.getValue().equals(type)) {
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

        if (Strings.isNotNullAndNotEmpty(type) && !Constants.LIVE_ROOM_TYPE_ALL.getValue().equals(type)) {
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

    @Override
    public Mono<Long> getCountByFilters(List<String> followings, String mediaType) {
        if (followings == null || followings.isEmpty()) {
            return Mono.just(0L);
        }

        Criteria criteria = Criteria.where("userId").in(followings)
                .and("status").is(Constants.STATUS_LIVE.getValue());

        if (!Constants.LIVE_ROOM_TYPE_ALL.getValue().equals(mediaType)) {
            criteria = criteria.and("type").is(mediaType);
        }

        Query query = new Query(criteria);

        return reactiveMongoTemplate.count(query, LiveRoomEntity.class);
    }

    public Mono<List<LiveRoom>> findByUserIds(List<String> userIds, Pageable pageable, String mediaType) {

        System.out.println("\n MediaType : "+mediaType+"\n");

        if (userIds == null || userIds.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }
        Criteria criteria = Criteria.where("userId").in(userIds)
                .and("status").is(Constants.STATUS_LIVE.getValue());

        if (!Constants.LIVE_ROOM_TYPE_ALL.getValue().equals(mediaType)) {
            criteria = criteria.and("type").is(mediaType);
        }

        Query query = new Query(criteria)
                .with(pageable)
                .with(Sort.by(Sort.Direction.DESC, "hostDailyGems"));

        return reactiveMongoTemplate.find(query, LiveRoomEntity.class)
                .map(entity -> modelMapper.map(entity, LiveRoom.class))
                .collectList();
    }
}
