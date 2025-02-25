package com.tanvir.features.level.adapter.out.persistence;

import com.tanvir.features.level.adapter.out.persistence.entity.LevelEntity;
import com.tanvir.features.level.adapter.out.persistence.repository.LevelRepository;
import com.tanvir.features.level.application.port.out.LevelPersistencePort;
import com.tanvir.features.level.domain.Level;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Component
@Slf4j
public class LevelPersistenceAdapter implements LevelPersistencePort {
    private final LevelRepository repository;
    private final ModelMapper modelMapper;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public LevelPersistenceAdapter(LevelRepository repository, ModelMapper modelMapper, ReactiveMongoTemplate reactiveMongoTemplate) {
        this.repository = repository;
        this.modelMapper = modelMapper;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    @Override
    public Mono<Level> getLevelDomainByLevel(int level) {
        return repository.findByLevel(level)
                .map(levelEntity -> modelMapper.map(levelEntity, Level.class))
                .doOnRequest(l -> log.info("Fetching level from mongo for level: {}", level))
//                .doOnSuccess(level1 -> log.info("Successfully fetched level from mongo: {}", level1))
                .doOnError(err -> log.error("Error while fetching level from mongo: {}", err.getMessage()));
    }

    @Override
    public Flux<Level> getAllLevels() {
        return repository.findAll()
                .map(levelEntity -> modelMapper.map(levelEntity, Level.class))
                .doOnRequest(l -> log.info("Fetching all levels from mongo"))
                .doOnComplete(() -> log.info("Successfully fetched all levels from mongo"))
                .doOnError(err -> log.error("Error while fetching all levels from mongo: {}", err.getMessage()));
    }

    @Override
    public Flux<Level> getLevelDomains(List<Integer> levels) {
        return repository.findByLevelIn(levels)
                .map(levelEntity -> modelMapper.map(levelEntity, Level.class));
    }

    @Override
    public Mono<Level> getLevelByExpValue(long expValue) {
        return reactiveMongoTemplate.findOne(
                    query(where("expValue").lte(expValue).and("nextLevelExpTargetValue").gt(expValue)),
                    LevelEntity.class)
                .map(levelEntity -> modelMapper.map(levelEntity, Level.class))
                .doOnError(err -> log.error("Error while fetching level from mongo: {}", err.getMessage()));
    }
}
