package com.tanvir.features.gift.adapter.out.persistence;

import com.tanvir.features.gift.adapter.out.persistence.entity.GiftEntity;
import com.tanvir.features.gift.adapter.out.persistence.repository.GiftRepository;
import com.tanvir.features.gift.application.port.out.GiftPersistencePort;
import com.tanvir.features.gift.domain.Gift;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Component
public class GiftPersistenceAdapter implements GiftPersistencePort {
    private final GiftRepository repository;
    private final ModelMapper modelMapper ;

    public GiftPersistenceAdapter(GiftRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Mono<Double> getGiftCostByGiftId(String id) {
        return repository.findById(id)
                .map(GiftEntity::getCost);
    }

    @Override
    public Flux<Gift> getGifts(Pageable pageable) {
        return repository.findAllBy(pageable)
                .doOnNext(giftEntity -> System.out.println("GiftEntity: " + giftEntity))
                .map(giftEntity -> modelMapper.map(giftEntity, Gift.class))
                .doOnError(throwable -> {
                    System.out.println("Error while getting all gifts from mongo");
                });

    }
}
