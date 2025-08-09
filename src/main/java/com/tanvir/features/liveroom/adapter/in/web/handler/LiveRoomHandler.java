package com.tanvir.features.liveroom.adapter.in.web.handler;

import com.tanvir.core.util.enums.AgoraTokenTypeEnum;
import com.tanvir.core.util.enums.Constants;
import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.core.util.exception.ErrorHandler;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.in.dto.request.*;
import com.tanvir.features.liveroom.application.port.in.dto.response.EarningResponseDto;
import com.tanvir.features.liveroom.application.port.in.dto.request.HostMicStatusRequestDto;
import com.tanvir.features.liveroom.domain.AllowedSeatNumber;
import com.tanvir.features.liveroom.domain.valueobject.Earning;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Map;

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
        return this.buildGridViewRequestDto(serverRequest)
                .flatMap(gridViewRequestDto -> liveRoomUseCase.getHomepage(gridViewRequestDto))
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto)
                );
    }

    private Mono<GridViewRequestDto> buildGridViewRequestDto(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String viewMode = serverRequest.queryParam(QueryParams.VIEW_MODE.getValue()).orElseThrow(() -> new IllegalArgumentException("viewMode is required"));
        String mediaType = serverRequest.queryParam(QueryParams.MEDIA_TYPE.getValue()).orElseThrow(() -> new IllegalArgumentException("mediaType is required"));

        List<String> liveRoomTypes = List.of(Constants.LIVE_ROOM_TYPE_AUDIO_UPPERCASE.getValue(),
                Constants.LIVE_ROOM_TYPE_VIDEO_UPPERCASE.getValue(),
                Constants.LIVE_ROOM_TYPE_ALL.getValue());

        if (!liveRoomTypes.contains(mediaType)) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "MediaType must be one of: AUDIO,VIDEO,ALL"));
        }

        List<String> validViewModes = List.of(Constants.VIEW_MODE_FOLLOWING.getValue(),
                Constants.VIEW_MODE_POPULAR.getValue(),
                Constants.VIEW_MODE_EXPLORE.getValue(),
                Constants.VIEW_MODE_SK.getValue(),
                Constants.VIEW_MODE_GUEST_CALL.getValue()
        );
        if (!validViewModes.contains(viewMode.toUpperCase())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid viewMode. Must be one of: FOLLOWING, POPULAR, EXPLORE, SK, GUEST_CALL."));
        }

        List<String> allAndAudioTypeLive = List.of(Constants.LIVE_ROOM_TYPE_AUDIO_UPPERCASE.getValue(),
                Constants.LIVE_ROOM_TYPE_ALL.getValue());

        if (allAndAudioTypeLive.contains(mediaType)
                && (viewMode.equalsIgnoreCase(Constants.VIEW_MODE_SK.getValue()) || viewMode.equalsIgnoreCase(Constants.VIEW_MODE_GUEST_CALL.getValue()))) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "ViewMode be SK or GUEST_CALL is available only for AUDIO or ALL type media."));
        }


        String country = null;
        if (viewMode.equalsIgnoreCase(Constants.VIEW_MODE_EXPLORE.getValue())) {
            country = serverRequest.queryParam(QueryParams.COUNTRY.getValue()).orElse(null);
        }

        int limit = Integer.parseInt(serverRequest.queryParam(QueryParams.LIMIT.getValue()).orElse("20"));
        int offSet = Integer.parseInt(serverRequest.queryParam(QueryParams.OFFSET.getValue()).orElse("0"));
        limit = Math.min(limit, 100);

        Pageable pageable = PageRequest.of(offSet, limit);

        return Mono.just(GridViewRequestDto.builder()
                .keycloakId(keycloakId)
                .viewMode(viewMode)
                .mediaType(this.buildUpperCaseMediaType(mediaType))
                .country(country)
                .pageable(pageable)
                .build());
    }

    private String buildUpperCaseMediaType(String mediaType) {
        if (mediaType.equals(Constants.LIVE_ROOM_TYPE_AUDIO_UPPERCASE.getValue()))
            return Constants.LIVE_ROOM_TYPE_AUDIO.getValue();
        else if (mediaType.equals(Constants.LIVE_ROOM_TYPE_VIDEO_UPPERCASE.getValue()))
            return Constants.LIVE_ROOM_TYPE_VIDEO.getValue();
        else
            return Constants.LIVE_ROOM_TYPE_ALL.getValue();
    }


    public Mono<ServerResponse> liveRoomById(ServerRequest serverRequest) {
        return liveRoomUseCase.getLiveRoomById_1(this.buildGridViewRequestDto_1(serverRequest))
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto)
                )
                .onErrorResume(ExceptionHandlerUtil.class, e -> ErrorHandler.buildErrorResponseForBusiness(e, serverRequest));
    }

    private GridViewRequestDto buildGridViewRequestDto_1(ServerRequest serverRequest) {

        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String liveRoomId = Optional.ofNullable(serverRequest.pathVariable(QueryParams.ID.getValue())).orElseThrow(() -> new IllegalArgumentException("Live room id is required"));

//        System.out.println();
        return GridViewRequestDto.builder()
                .keycloakId(liveRoomId)
                .build();
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

    public Mono<ServerResponse> setEnableAutoJoinAudioStream(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("Keycloak id is required"));
        String liveRoomId = serverRequest.pathVariable("id");

        return serverRequest
                .bodyToMono(JoinPermissionRequestDTO.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(liveRoomId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::setEnableAutoJoinAudioStream)
                .flatMap(dto -> ServerResponse
                        .created(serverRequest.uri())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto));
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

    public Mono<ServerResponse> updateJoinRequest(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("The Keycloak ID is mandatory."));
        String liveRoomId = serverRequest.pathVariable("id");
        String requestId = serverRequest.pathVariable(QueryParams.REQUEST_ID.getValue());


        return serverRequest
                .bodyToMono(JoinCallRequestUpdateDto.class)
                .map(requestDto -> {
                    requestDto.setKeycloakId(keycloakId);
                    requestDto.setLiveRoomId(liveRoomId);
                    requestDto.setRequestId(requestId);
                    return requestDto;
                })
                .flatMap(liveRoomUseCase::updateJoinCall)
                .flatMap(dto -> ServerResponse
                        .ok()
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

    public Mono<ServerResponse> earnings(ServerRequest serverRequest) {

        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("The Keycloak ID is mandatory and must be provided."));

        log.info("Processing earnings for keycloakId: {}", keycloakId);

        if (keycloakId.trim().isEmpty()) {
            log.warn("Invalid keycloakId provided");
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("message", "User keycloak ID must not be empty", "error", true));
        }

        return liveRoomUseCase.userEarning(keycloakId)
                .switchIfEmpty(Mono.just(Earning.builder().build())) // Handle empty results
                .map(earning -> {
                    log.info("Earnings fetched successfully for keycloakId: {}", keycloakId);
                    return EarningResponseDto.builder()
                            .message("Earning details fetched successfully")
                            .data(EarningResponseDto.EarningModel.builder()
                                    .month(earning.getMonth())
                                    .year(earning.getYear())
                                    .hostType(earning.getHostType())
                                    .gems(earning.getGems())
                                    .gemsString(earning.getGemsString())
                                    .duration(earning.getDuration())
                                    .durationString(earning.getDurationString())
                                    .validDays(earning.getValidDays())
                                    .bonus(earning.getBonus())
                                    .bonusString(earning.getBonusString())
                                    .build())
                            .error(false)
                            .count(1)
                            .build();
                })
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                .onErrorResume(e -> {
                    log.error("Error processing earnings for keycloakId {}: {}", keycloakId, e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(EarningResponseDto.builder()
                                    .message("Failed to fetch earning details")
                                    .error(true)
                                    .build());
                });
    }

    public Mono<ServerResponse> setMicStatus(ServerRequest serverRequest) {

        String liveRoomId = serverRequest.pathVariable(QueryParams.ID.getValue());

        return serverRequest.bodyToMono(HostMicStatusRequestDto.class)
                .flatMap(hostMicStatusDto -> liveRoomUseCase.setMicStatus(liveRoomId, hostMicStatusDto.getMicOn()))
                .flatMap(hostResponseDto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(hostResponseDto)
                );
    }
    public Mono<ServerResponse> enableAutoJoinAudioStream(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(JoinCallRequestDto.class)
                .flatMap(liveRoomUseCase::autoJoinProcess)
                .flatMap(responseDto -> ServerResponse.ok().bodyValue(responseDto));
    }
}
