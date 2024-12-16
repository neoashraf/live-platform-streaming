package com.tanvir.features.liveroomsummary.adapter.in.web.router;

import com.tanvir.features.liveroomsummary.adapter.in.web.handler.LiveRoomSummaryHandler;
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
public class LiveRoomSummaryRouter {
    private final LiveRoomSummaryHandler handler;

    @Bean
    public RouterFunction<ServerResponse> liveRoomSummaryRouterConfig() {
        return route()
                .path(MAX_LIVE_HOME_BASE_URL,
                        builder -> builder
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .GET(EARNINGS, handler::getHostEarnings)
                                )
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .GET("/update", handler::updateOldLiveRoomSummary)
                                )
                )
                .build();
    }
}
