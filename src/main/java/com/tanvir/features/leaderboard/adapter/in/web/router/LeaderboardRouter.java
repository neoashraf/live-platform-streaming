package com.tanvir.features.leaderboard.adapter.in.web.router;

import com.tanvir.features.leaderboard.adapter.in.web.handler.LeaderboardHandler;
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
public class LeaderboardRouter {
    private final LeaderboardHandler handler;

    @Bean
    public RouterFunction<ServerResponse> leaderboardRouterConfig() {
        return route()
                .path(MAX_LIVE_HOME_BASE_URL,
                        builder -> builder
                            .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                nestedBuilder
                                    .GET(LEADERBOARD.concat(GLOBAL).concat(FANS), handler::getGlobalFanLeaderboard)
                            )
                            .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                nestedBuilder
                                    .GET(LEADERBOARD.concat(HOST).concat(FANS), handler::getHostFanLeaderboard)
                            )
                            .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                nestedBuilder
                                    .GET(LEADERBOARD.concat(HOST), handler::getHostLeaderboard)
                            )
                            .nest(RequestPredicates.accept(MediaType.APPLICATION_JSON), nestedBuilder ->
                                nestedBuilder
                                    .GET(LEADERBOARD.concat(AGENCY), handler::getAgencyLeaderboard)
                            )
                )
                .build();
    }
}
