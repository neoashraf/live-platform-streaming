package com.tanvir.features.agora.handler;

import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.features.agora.service.AgoraService;
import com.tanvir.features.agora.service.AgoraTokenGenerationResponseDto;
import com.tanvir.features.agora.service.AgoraTokenRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.testng.util.Strings;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AgoraHandler {

    private final AgoraService agoraService;

    public AgoraHandler(AgoraService agoraService) {
        this.agoraService = agoraService;
    }

    public Mono<ServerResponse> generateToken(ServerRequest serverRequest) {

        return serverRequest
                .bodyToMono(AgoraTokenRequestDto.class)
                .filter(agoraTokenRequestDto -> Strings.isNotNullAndNotEmpty(agoraTokenRequestDto.getTokenType()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(QueryParams.TOKEN_TYPE.getValue() + " is required")))
                .flatMap(agoraService::generateToken)
                .map(agoraTokenResponseDto -> AgoraTokenGenerationResponseDto.builder()
                        .message("Token generated successfully")
                        .data(agoraTokenResponseDto.getData().get(0))
                        .count(agoraTokenResponseDto.getCount())
                        .error(agoraTokenResponseDto.isError())
                        .build())
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

}
