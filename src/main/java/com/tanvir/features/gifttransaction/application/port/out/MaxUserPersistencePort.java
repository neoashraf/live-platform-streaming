package com.tanvir.features.gifttransaction.application.port.out;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.MaxUserEntity;
import reactor.core.publisher.Mono;

public interface MaxUserPersistencePort {
    Mono<MaxUserEntity> getMaxUserEntityByUserId(String userId);
    Mono<MaxUserEntity> saveMaxUserEntity(MaxUserEntity maxUserEntity);
}
