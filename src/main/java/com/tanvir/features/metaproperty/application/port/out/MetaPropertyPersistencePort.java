package com.tanvir.features.metaproperty.application.port.out;

import com.tanvir.features.metaproperty.domain.MetaProperty;
import reactor.core.publisher.Mono;

public interface MetaPropertyPersistencePort {
    Mono<MetaProperty> getMetaPropertyByDescription(String description);
}
