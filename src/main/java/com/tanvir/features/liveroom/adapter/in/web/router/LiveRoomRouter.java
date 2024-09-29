package com.tanvir.features.liveroom.adapter.in.web.router;

import com.tanvir.features.liveroom.adapter.in.web.handler.LiveRoomHandler;
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
public class LiveRoomRouter {
    private final LiveRoomHandler handler;

    @Bean
    public RouterFunction<ServerResponse> liveRoomRouterConfig() {
        return route()
                .path(MAX_LIVE_HOME_BASE_URL,
                        builder -> builder
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .GET(LIVE_ROOMS, handler::homepage)
                                )
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .POST(LIVE_ROOMS, handler::createStream)
                                )
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .POST(LIVE_ROOMS.concat(ID).concat(JOIN), handler::joinStream)
                                )
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .POST(LIVE_ROOMS.concat(ID).concat(LEAVE), handler::leaveStream)
                                )
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .DELETE(LIVE_ROOMS.concat(ID), handler::endStream)
                                )
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .POST(LIVE_ROOMS.concat(ID).concat(KICK), handler::kickOutUser)
                                )
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .POST(LIVE_ROOMS.concat(ID).concat(COMMENT), handler::comment)
                                )
                )
                .build();
    }
}
