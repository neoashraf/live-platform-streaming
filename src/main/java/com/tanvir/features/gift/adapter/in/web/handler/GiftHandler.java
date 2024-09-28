package com.tanvir.features.gift.adapter.in.web.handler;

import com.tanvir.features.gift.application.port.in.GiftUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
@Component
@Slf4j
public class GiftHandler {
    private final GiftUseCase giftUseCase;

    public GiftHandler(GiftUseCase giftUseCase) {
        this.giftUseCase = giftUseCase;
    }


    public Mono<ServerResponse> getGifts(ServerRequest serverRequest) {
        Integer offSet = Integer.parseInt(serverRequest.queryParam("offset").orElse("0"));
        Integer limit = Integer.parseInt(serverRequest.queryParam("limit").orElse("100"));
        return giftUseCase.getGifts(offSet,limit)
                .flatMap(giftResponseDto -> ServerResponse
                        .ok()
                        .bodyValue(giftResponseDto));
    }
}
