package com.tanvir.features.agency;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface AgencyRepository extends ReactiveMongoRepository<AgencyEntity, String> {
    Flux<AgencyEntity> findAllByMaxIdIn(Iterable<String> agencyIds);
}
