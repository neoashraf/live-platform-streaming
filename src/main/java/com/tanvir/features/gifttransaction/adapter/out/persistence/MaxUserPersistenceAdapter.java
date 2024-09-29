package com.tanvir.features.gifttransaction.adapter.out.persistence;

import com.tanvir.features.gifttransaction.adapter.out.persistence.entity.MaxUserEntity;
import com.tanvir.features.gifttransaction.adapter.out.persistence.repository.MaxUserRepository;
import com.tanvir.features.gifttransaction.application.port.out.MaxUserPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MaxUserPersistenceAdapter implements MaxUserPersistencePort {
    private final MaxUserRepository maxUserRepository;

    public MaxUserPersistenceAdapter(MaxUserRepository maxUserRepository) {
        this.maxUserRepository = maxUserRepository;
    }

    @Override
    public Mono<MaxUserEntity> getMaxUserEntityByUserId(String userId) {
        return maxUserRepository.findMaxUserEntityByUserId(userId);
    }

    @Override
    public Mono<MaxUserEntity> saveMaxUserEntity(MaxUserEntity maxUserEntity) {
        return maxUserRepository.save(maxUserEntity);
    }
}
