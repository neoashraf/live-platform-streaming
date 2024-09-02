package com.tanvir.features.liveroom.adapter.in.web.handler;

import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.in.dto.request.KickOutUserRequestDto;
import com.tanvir.features.liveroom.application.port.in.dto.request.LiveRoomEntryLeaveRequestDto;
import com.tanvir.features.liveroom.application.port.in.dto.request.LiveRoomRequestDto;
import com.tanvir.features.liveroom.domain.valueobject.Fan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveRoomHandler {
    private final LiveRoomUseCase liveRoomUseCase;

    public Mono<ServerResponse> createStream(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        return serverRequest
                .bodyToMono(LiveRoomRequestDto.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::createStream)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> joinStream(ServerRequest serverRequest) {
        log.info("Joining stream");
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String id = serverRequest.pathVariable("id");
        return serverRequest
                .bodyToMono(Fan.class)
                .map(fan -> LiveRoomEntryLeaveRequestDto.builder().fan(fan).build())
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(id);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::joinStream)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> leaveStream(ServerRequest serverRequest) {
        log.info("Leaving stream");
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String id = serverRequest.pathVariable("id");
        return serverRequest
                .bodyToMono(Fan.class)
                .map(fan -> LiveRoomEntryLeaveRequestDto.builder().fan(fan).build())
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(id);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::leaveStream)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> endStream(ServerRequest serverRequest) {
        log.info("Ending stream");
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String liveRoomId = serverRequest.pathVariable("id");
        return liveRoomUseCase.endStream(liveRoomId, keycloakId)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> kickOutUser(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String liveRoomId = serverRequest.pathVariable("id");
        return serverRequest
                .bodyToMono(KickOutUserRequestDto.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(liveRoomId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::kickOutUser)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }
}
