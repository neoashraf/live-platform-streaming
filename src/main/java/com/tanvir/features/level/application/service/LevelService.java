package com.tanvir.features.level.application.service;

import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.level.application.port.out.LevelPersistencePort;
import com.tanvir.features.level.domain.Level;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class LevelService implements LevelUseCase {

    private final LevelPersistencePort port;

    public LevelService(LevelPersistencePort port) {
        this.port = port;
    }

    @Override
    public Mono<Level> getLevelDomainByLevel(int level) {
        return port.getLevelDomainByLevel(level)
//                .doOnRequest(l -> log.info("Fetching level from mongo for level: {}", level))
//                .doOnSuccess(level1 -> log.info("Successfully fetched level from mongo: {}", level1));
                .doOnError(err -> log.error("Error while fetching level from mongo: {}", err.getMessage()));
    }

    @Override
    public Mono<List<Level>> getAllLevels() {
        return port.getAllLevels()
                .collectList();
    }

    @Override
    public Flux<Level> getLevelDomains(List<Integer> levels) {
        return port.getLevelDomains(levels)
                .doOnRequest(l -> log.info("Fetching levels from mongo for levels: {}", levels))
                .doOnComplete(() -> log.info("Successfully fetched levels from mongo"))
                .doOnError(err -> log.error("Error while fetching levels from mongo: {}", err.getMessage()));
    }

    @Override
    public Mono<Level> getLevelByExpValue(long expValue) {
        return port.getLevelByExpValue(expValue)
                .doOnError(err -> log.error("Error while fetching level from mongo: {}", err.getMessage()));
    }
}
