package com.tanvir.features.liveroom.adapter.in.web.handler;

import com.tanvir.core.util.enums.AgoraTokenTypeEnum;
import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.core.util.exception.ErrorHandler;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.in.dto.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
//        String tokenType = serverRequest.queryParam(QueryParams.TOKEN_TYPE.getValue()).orElseThrow(() -> new IllegalArgumentException("Token type is required"));
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

    public Mono<ServerResponse> createAudioStream(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        return serverRequest
                .bodyToMono(LiveRoomRequestDto.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::createAudioStream)
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
        return liveRoomUseCase.joinStream(LiveRoomViewerRequestDto
                        .builder()
                        .liveRoomId(id)
                        .keycloakId(keycloakId)
                        .tokenType(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue())
                        .build())
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> joinAudioStream(ServerRequest serverRequest) {
        log.info("Joining audio stream");
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String id = serverRequest.pathVariable("id");
        return liveRoomUseCase.joinAudioStream(LiveRoomViewerRequestDto
                        .builder()
                        .liveRoomId(id)
                        .keycloakId(keycloakId)
                        .tokenType(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue())
                        .build())
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
        return Mono.just(LiveRoomViewerRequestDto.builder().build())
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

    public Mono<ServerResponse> comment(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String liveRoomId = serverRequest.pathVariable("id");
        return serverRequest
                .bodyToMono(LiveRoomViewerRequestDto.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(liveRoomId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::comment)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> homepage(ServerRequest serverRequest) {
        return liveRoomUseCase.getHomepage(this.buildGridViewRequestDto(serverRequest))
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    private GridViewRequestDto buildGridViewRequestDto(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String viewMode = serverRequest.queryParam(QueryParams.VIEW_MODE.getValue()).orElseThrow(() -> new IllegalArgumentException("viewMode is required"));
        String country = serverRequest.queryParam(QueryParams.COUNTRY.getValue()).orElse(null);
        int limit = Integer.parseInt(serverRequest.queryParam(QueryParams.LIMIT.getValue()).orElse("20"));
        int offSet = Integer.parseInt(serverRequest.queryParam(QueryParams.OFFSET.getValue()).orElse("0"));
        limit = Math.min(limit, 100);
        Pageable pageable = PageRequest.of(offSet, limit);
        return GridViewRequestDto.builder()
                .keycloakId(keycloakId)
                .viewMode(viewMode)
                .country(country)
                .pageable(pageable)
                .build();
    }


    public Mono<ServerResponse> liveRoomById(ServerRequest serverRequest) {
        return liveRoomUseCase.getLiveRoomById_1(this.buildGridViewRequestDto_1(serverRequest))
                .flatMap(dto->ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto)
                )
                .onErrorResume(ExceptionHandlerUtil.class, e -> ErrorHandler.buildErrorResponseForBusiness(e, serverRequest));
    }

    public Mono<ServerResponse> setJoinPermission(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String liveRoomId = serverRequest.pathVariable("id");

        return serverRequest
                .bodyToMono(JoinPermissionRequestDTO.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(liveRoomId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::setJoinPermission)
                .flatMap(dto -> ServerResponse
                        .created(serverRequest.uri())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> joinRequest(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("The Keycloak ID is mandatory."));
        String liveRoomId = serverRequest.pathVariable("id");

        return serverRequest
                .bodyToMono(JoinCallRequestDto.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(liveRoomId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::requestJoinCall)
                .flatMap(dto -> ServerResponse
                        .created(serverRequest.uri())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> processJoinRequest(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("The Keycloak ID is mandatory."));
        String liveRoomId = serverRequest.pathVariable(QueryParams.ID.getValue());
        String requestId = serverRequest.pathVariable(QueryParams.REQUEST_ID.getValue());

        return serverRequest
                .bodyToMono(JoinCallRequestDto.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(liveRoomId);
                    requestDto.setRequestId(requestId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::processJoinCall)
                .flatMap(dto -> ServerResponse
                        .created(serverRequest.uri())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> startJoinCall(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("The Keycloak ID is mandatory."));
        String liveRoomId = serverRequest.pathVariable(QueryParams.ID.getValue());
        String requestId = serverRequest.pathVariable(QueryParams.REQUEST_ID.getValue());

        return serverRequest
                .bodyToMono(JoinCallRequestDto.class)
                .switchIfEmpty(Mono.just(JoinCallRequestDto.builder().build()))
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(liveRoomId);
                    requestDto.setRequestId(requestId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::startJoinCall)
                .flatMap(dto -> ServerResponse
                        .created(serverRequest.uri())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    public Mono<ServerResponse> closeJoinedCallRequest(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("The Keycloak ID is mandatory."));
        String liveRoomId = serverRequest.pathVariable(QueryParams.ID.getValue());
        String requestId = serverRequest.pathVariable(QueryParams.REQUEST_ID.getValue());

        JoinCallRequestDto joinCallRequestDto = JoinCallRequestDto.builder()
                .keycloakId(keycloakId)
                .liveRoomId(liveRoomId)
                .requestId(requestId)
                .build();

        return liveRoomUseCase.closeJoinedCall(joinCallRequestDto)
                .flatMap(dto -> ServerResponse
                        .created(serverRequest.uri())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto));
    }


}
