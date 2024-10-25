package com.tanvir.features.liveroomsummary.adapter.in.web.handler;

import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.features.liveroomsummary.application.port.in.LiveRoomSummaryUseCase;
import com.tanvir.features.liveroomsummary.domain.dto.HostEarningRequestDto;
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
public class LiveRoomSummaryHandler {
    private final LiveRoomSummaryUseCase liveRoomSummaryUseCase;

    public Mono<ServerResponse> getHostEarnings(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("The Keycloak ID is mandatory."));
        String month = serverRequest.queryParam(QueryParams.MONTH.getValue()).orElseThrow(() -> new IllegalArgumentException("The month value is mandatory."));
        String year = serverRequest.queryParam(QueryParams.YEAR.getValue()).orElseThrow(() -> new IllegalArgumentException("The year value is mandatory."));

        HostEarningRequestDto requestDto = HostEarningRequestDto.builder()
                .keycloakId(keycloakId)
                .month(Integer.parseInt(month))
                .year(Integer.parseInt(year))
                .build();

        return liveRoomSummaryUseCase.getHostEarnings(requestDto)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto));
    }


}
