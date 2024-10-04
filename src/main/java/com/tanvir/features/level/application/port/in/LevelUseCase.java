package com.tanvir.features.level.application.port.in;

import com.tanvir.features.level.domain.Level;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LevelUseCase {
    Mono<Level> getLevelDomainByLevel(int level);
    Mono<List<Level>> getAllLevels();
}
