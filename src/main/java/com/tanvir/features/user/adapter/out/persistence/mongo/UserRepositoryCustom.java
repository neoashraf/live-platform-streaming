package com.tanvir.features.user.adapter.out.persistence.mongo;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

public interface UserRepositoryCustom {
    public Flux<UserEntity> findAllByFilters(
            String role, String country, String gender, String active, String searchKey, String maxId, Pageable pageable);
}
