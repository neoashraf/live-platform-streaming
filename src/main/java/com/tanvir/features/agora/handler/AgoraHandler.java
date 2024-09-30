package com.tanvir.features.agora.handler;

import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.features.agora.service.AgoraService;
import com.tanvir.features.agora.service.AgoraTokenRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AgoraHandler {

    private final AgoraService agoraService;

    public AgoraHandler(AgoraService agoraService) {
        this.agoraService = agoraService;
    }

    public Mono<ServerResponse> generateToken(ServerRequest serverRequest) {
//        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));

        String tokenType = serverRequest.queryParam(QueryParams.TOKEN_TYPE.getValue()).orElseThrow(() -> new IllegalArgumentException("Token type is required"));
        return serverRequest
                .bodyToMono(AgoraTokenRequestDto.class)
                .map(requestDto -> {
                    requestDto.setTokenType(tokenType);
                    return requestDto;
                })
                .flatMap(agoraService::generateToken)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

}
