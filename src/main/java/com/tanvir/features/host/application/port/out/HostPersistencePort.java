package com.tanvir.features.host.application.port.out;

import com.tanvir.features.host.application.port.in.dto.request.HostRequestDto;
import com.tanvir.features.host.domain.Host;
import com.tanvir.features.user.domain.User;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HostPersistencePort {
    Mono<Host> getHostByUserId(String userId);
    Mono<Host> saveHost(Host host);
    Mono<Host> addMoreGems(String userId, int gemsAmount);
}
