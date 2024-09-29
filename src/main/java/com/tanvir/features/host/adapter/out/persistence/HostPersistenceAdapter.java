package com.tanvir.features.host.adapter.out.persistence;

import com.tanvir.features.host.adapter.out.persistence.entity.HostEntity;
import com.tanvir.features.host.adapter.out.persistence.repository.HostRepository;
import com.tanvir.features.host.adapter.out.persistence.repository.HostRepositoryCustom;
import com.tanvir.features.host.application.port.in.dto.request.HostRequestDto;
import com.tanvir.features.host.application.port.out.HostPersistencePort;
import com.tanvir.features.host.domain.Host;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Condition;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.testng.util.Strings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class HostPersistenceAdapter implements HostPersistencePort {

    private final HostRepository repository;
    private final HostRepositoryCustom customRepository;
    private final ModelMapper modelMapper;

    public HostPersistenceAdapter(HostRepository repository, HostRepositoryCustom customRepository, ModelMapper modelMapper) {
        this.repository = repository;
        this.customRepository = customRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Mono<Host> getHostByUserId(String userId) {
        return repository.findByUserId(userId)
            .map(entity -> modelMapper.map(entity, Host.class))
            .doOnRequest(value -> log.info("Getting host from mongo with userId {}", userId))
            .doOnError(throwable -> log.error("Error while getting host from mongo: {}", throwable.getMessage()))
            .doOnSuccess(host -> log.info("Got host from mongo {}", host));
    }

    @Override
    public Mono<Host> saveHost(Host host) {
        return repository.save(modelMapper.map(host, HostEntity.class))
            .map(entity -> modelMapper.map(entity, Host.class))
            .doOnRequest(value -> log.info("Saving host to mongo"))
            .doOnError(throwable -> log.error("Error while saving host to mongo: {}", throwable.getMessage()))
            .doOnSuccess(host1 -> log.info("Saved host to mongo {}", host1));
    }
}
