package com.tanvir.features.metaproperty.application.service;

import com.tanvir.features.metaproperty.application.port.in.MetaPropertyUseCase;
import com.tanvir.features.metaproperty.application.port.out.MetaPropertyPersistencePort;
import com.tanvir.features.metaproperty.domain.MetaProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MetaPropertyService implements MetaPropertyUseCase {

    private final MetaPropertyPersistencePort port;

    public MetaPropertyService(MetaPropertyPersistencePort port) {
        this.port = port;
    }

    @Override
    public Mono<MetaProperty> getMetaPropertyByDescription(String description) {
        return port.getMetaPropertyByDescription(description)
                .switchIfEmpty(Mono.just(
                    MetaProperty
                        .builder()
                        .popularIndex(1)
                        .starIndex(10000)
                        .gemsConversionRate(50.0)
                        .build()));
    }
}
