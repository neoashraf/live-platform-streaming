package com.tanvir.features.gifttransaction.adapter.in.handler;

import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCase;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.BeanTransactionRequestDto;
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

}
