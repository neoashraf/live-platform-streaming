package com.tanvir.features.gifttransaction.adapter.in.router;

import com.tanvir.features.gifttransaction.adapter.in.handler.GiftTransactionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.tanvir.core.routes.RouteNames.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class GiftTransactionRouter {
    private final GiftTransactionHandler handler;

    @Bean
    public RouterFunction<ServerResponse> giftTransactionRouterConfig() {
        return route()
                .path(MAX_LIVE_HOME_BASE_URL,
                        builder -> builder
                            .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                nestedBuilder
                                    .POST(GIFTS.concat(SEND), handler::sendGifts)
                            )
                            .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                nestedBuilder
                                    .GET(GIFT_TRANSACTIONS, handler::getGiftTransactions)
                            )
                )
                .build();
    }
}
