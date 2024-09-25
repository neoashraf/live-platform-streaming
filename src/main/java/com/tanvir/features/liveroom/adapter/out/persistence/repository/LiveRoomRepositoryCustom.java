package com.tanvir.features.liveroom.adapter.out.persistence.repository;

import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface LiveRoomRepositoryCustom {
    public Flux<LiveRoomEntity> findAllByFilters(String type, String status, String country, Pageable pageable);
    public Mono<Long> getCountByFilters(String type, String status, String country);
}
