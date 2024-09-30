package com.tanvir.features.dailyidgenerator;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ScheduledIdRepository extends ReactiveMongoRepository<ScheduledId, String> {
}
