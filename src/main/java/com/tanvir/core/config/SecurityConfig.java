package com.tanvir.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
        return httpSecurity
                .csrf().disable()
                .cors().and()
                .exceptionHandling()
                .authenticationEntryPoint((serverWebExchange, e) -> Mono.fromRunnable(() -> serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)))
                .accessDeniedHandler((serverWebExchange, e) -> Mono.fromRunnable(() -> serverWebExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)))
                .and()
                .authorizeExchange()
                .anyExchange().permitAll()
                .and()
                .formLogin().disable()
                .httpBasic().disable()
                .build();

    }
}
