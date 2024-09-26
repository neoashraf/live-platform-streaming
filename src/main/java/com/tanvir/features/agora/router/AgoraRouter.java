package com.tanvir.features.agora.router;

import com.tanvir.features.agora.handler.AgoraHandler;
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
public class AgoraRouter {
    private final AgoraHandler handler;

    @Bean
    public RouterFunction<ServerResponse> agoraRouterConfig() {
        return route()
                .path(MAX_LIVE_HOME_BASE_URL,
                        builder -> builder
                                .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                        nestedBuilder
                                                .POST(AGORA.concat(TOKEN), handler::generateToken)
                                )
                )
                .build();
    }
}
