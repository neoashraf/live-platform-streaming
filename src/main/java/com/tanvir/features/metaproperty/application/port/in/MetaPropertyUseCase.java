package com.tanvir.features.metaproperty.application.port.in;

import com.tanvir.features.metaproperty.domain.MetaProperty;
import reactor.core.publisher.Mono;

public interface MetaPropertyUseCase {
    Mono<MetaProperty> getMetaPropertyByDescription(String description);
}
