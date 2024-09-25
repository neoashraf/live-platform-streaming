package com.tanvir.features.host.adapter.out.persistence.repository;

import com.tanvir.features.host.adapter.out.persistence.entity.HostEntity;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

public interface HostRepositoryCustom {
    public Flux<HostEntity> findAllByFilters(
            String country, String gender, String active, String searchKey, String maxId, String agencyMaxId, String hostType, Pageable pageable);
}
