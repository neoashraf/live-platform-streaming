package com.tanvir.features.metaproperty.adapter.out.persistence;

import com.tanvir.features.metaproperty.application.port.out.MetaPropertyPersistencePort;
import com.tanvir.features.metaproperty.domain.MetaProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MetaPropertyPersistenceAdapter implements MetaPropertyPersistencePort {
    @Override
    public Mono<MetaProperty> getMetaPropertyByDescription(String description) {
        return Mono.empty();
    }
}
