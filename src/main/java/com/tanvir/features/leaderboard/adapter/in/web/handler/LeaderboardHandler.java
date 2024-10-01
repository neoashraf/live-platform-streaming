package com.tanvir.features.leaderboard.adapter.in.web.handler;

import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCase;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.SendGiftRequestDto;
import com.tanvir.features.leaderboard.application.port.in.LeaderboardUseCase;
import com.tanvir.features.leaderboard.application.port.in.dto.request.LeaderboardRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardHandler {

    private final LeaderboardUseCase leaderboardUseCase;

    public Mono<ServerResponse> getFanLeaderboard(ServerRequest serverRequest) {
        return this.buildBeanTransactionRequestDto(serverRequest)
                .flatMap(leaderboardUseCase::getFanLeaderBoard)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
                /*.onErrorResume(ExceptionHandlerUtil.class, e -> ErrorHandler.buildErrorResponseForBusiness(e, serverRequest))
                .onErrorResume(Predicate.not(ExceptionHandlerUtil.class::isInstance), e -> ErrorHandler.buildErrorResponseForUncaught(e, serverRequest));*/
    }


    private Mono<LeaderboardRequestDto> buildBeanTransactionRequestDto(ServerRequest serverRequest) {
        int limit = Integer.parseInt(serverRequest.queryParam(QueryParams.LIMIT.getValue()).orElse("10"));
        String agencyMaxId = serverRequest.queryParam(QueryParams.AGENCY_MAX_ID.getValue()).orElse("");
        String createdAfterString = serverRequest.queryParam(QueryParams.CREATED_AFTER.getValue()).orElseThrow(() -> new IllegalArgumentException("createdAfter is required"));
        String createdBeforeString = serverRequest.queryParam(QueryParams.CREATED_BEFORE.getValue()).orElseThrow(() -> new IllegalArgumentException("createdBefore is required"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        createdAfterString = createdAfterString.replace(" ", "+");
        createdBeforeString = createdBeforeString.replace(" ", "+");
        LocalDateTime createdAfter = LocalDateTime.parse(createdAfterString, formatter);
        LocalDateTime createdBefore = LocalDateTime.parse(createdBeforeString, formatter);

        return Mono.just(LeaderboardRequestDto
                .builder()
                .userId(serverRequest.queryParam(QueryParams.USER_ID.getValue()).orElse(""))
                .agencyMaxId(agencyMaxId)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .limit(limit)
                .build());

    }

}
