package com.tanvir.features.gifttransaction.adapter.in.handler;

import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCase;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.SendGiftRequestDto;
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
public class GiftTransactionHandler {

    private final GiftTransactionUseCase giftTransactionUseCase;

    public Mono<ServerResponse> sendGifts(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(SendGiftRequestDto.class)
                .map(requestDto -> {
                    String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("keycloakId is required"));
                    requestDto.setKeycloakId(keycloakId);
                    return requestDto;
                })
                .flatMap(giftTransactionUseCase::sendGifts)
                .flatMap(dto -> ServerResponse
                        .created(serverRequest.uri())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
                /*.onErrorResume(ExceptionHandlerUtil.class, e -> ErrorHandler.buildErrorResponseForBusiness(e, serverRequest))
                .onErrorResume(Predicate.not(ExceptionHandlerUtil.class::isInstance), e -> ErrorHandler.buildErrorResponseForUncaught(e, serverRequest));*/
    }

    public Mono<ServerResponse> getGiftTransactions(ServerRequest serverRequest) {
        return this.buildBeanTransactionRequestDto(serverRequest)
                .flatMap(giftTransactionUseCase::getGiftTransactions)
                .flatMap(dto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto))
                ;
    }

    private Mono<GiftTransactionRequestDto> buildBeanTransactionRequestDto(ServerRequest serverRequest) {
        String keycloakId = serverRequest.queryParam(QueryParams.KEYCLOAK_ID.getValue()).orElseThrow(() -> new IllegalArgumentException("keycloakId is required"));
        String transactionType = serverRequest.queryParam(QueryParams.TRANSACTION_TYPE.getValue()).orElse("");
        String searchKey = serverRequest.queryParam(QueryParams.SEARCH_KEY.getValue()).orElse("");
        int limit = Integer.parseInt(serverRequest.queryParam(QueryParams.LIMIT.getValue()).orElse("10"));
        int offSet = Integer.parseInt(serverRequest.queryParam(QueryParams.OFFSET.getValue()).orElse("0"));
        limit = Math.min(limit, 100);
        Pageable pageable = PageRequest.of(offSet, limit);

        String createdAfterString = serverRequest.queryParam(QueryParams.CREATED_AFTER.getValue()).orElseThrow(() -> new IllegalArgumentException("createdAfter is required"));
        String createdBeforeString = serverRequest.queryParam(QueryParams.CREATED_BEFORE.getValue()).orElseThrow(() -> new IllegalArgumentException("createdBefore is required"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        createdAfterString = createdAfterString.replace(" ", "+");
        createdBeforeString = createdBeforeString.replace(" ", "+");
        LocalDateTime createdAfter = LocalDateTime.parse(createdAfterString, formatter);
        LocalDateTime createdBefore = LocalDateTime.parse(createdBeforeString, formatter);

        return Mono.just(GiftTransactionRequestDto
                .builder()
                .keycloakId(keycloakId)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .searchKey(searchKey)
                .pageable(pageable)
                .transactionType(transactionType)
                .build());

    }

}
