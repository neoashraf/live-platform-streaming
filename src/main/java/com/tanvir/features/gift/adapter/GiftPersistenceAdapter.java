package com.tanvir.features.gift.adapter;

import com.tanvir.features.gift.adapter.out.persistence.entity.GiftEntity;
import com.tanvir.features.gift.adapter.out.persistence.repository.GiftRepository;
import com.tanvir.features.gift.application.port.out.GiftPersistencePort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
@Component
public class GiftPersistenceAdapter implements GiftPersistencePort {
    private final GiftRepository repository;

    public GiftPersistenceAdapter(GiftRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Double> getGiftCostByGiftId(String id) {
        return repository.findById(id)
                .map(GiftEntity::getCost);
    }
}
