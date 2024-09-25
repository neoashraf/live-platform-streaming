package com.tanvir.features.host.application.service;
import com.tanvir.features.host.application.port.in.HostUseCase;
import com.tanvir.features.host.application.port.out.HostPersistencePort;
import com.tanvir.features.host.domain.Host;
import com.tanvir.features.user.application.port.in.UserUseCase;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class HostService implements HostUseCase {
    private final HostPersistencePort port;
    private final UserUseCase userUseCase;
    private final ModelMapper modelMapper;
    private final TransactionalOperator transactionalOperator;

    public HostService(HostPersistencePort port, UserUseCase userUseCase, ModelMapper modelMapper, TransactionalOperator transactionalOperator) {
        this.port = port;
        this.userUseCase = userUseCase;
        this.modelMapper = modelMapper;
        this.transactionalOperator = transactionalOperator;
    }


    @Override
    public Mono<Host> getHostByUserId(String userId) {
        return port.getHostByUserId(userId);
    }
}
