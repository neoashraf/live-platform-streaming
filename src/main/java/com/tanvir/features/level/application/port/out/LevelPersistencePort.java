package com.tanvir.features.level.application.port.out;

import com.tanvir.features.level.domain.Level;
import reactor.core.publisher.Mono;

public interface LevelPersistencePort {
    Mono<Level> getLevelDomainByLevel(int level);
}
