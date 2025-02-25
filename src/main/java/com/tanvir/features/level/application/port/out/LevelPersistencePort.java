package com.tanvir.features.level.application.port.out;

import com.tanvir.features.level.domain.Level;
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LevelPersistencePort {
    Mono<Level> getLevelDomainByLevel(int level);
    Flux<Level> getAllLevels();

    Flux<Level> getLevelDomains(List<Integer> levels);

    Mono<Level> getLevelByExpValue(long expValue);
}
