package com.tanvir.features.host.application.port.in;


import com.tanvir.features.host.domain.Host;
import reactor.core.publisher.Mono;

public interface HostUseCase {
    Mono<Host> getHostByUserId(String userId);
}
