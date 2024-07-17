package com.tanvir.core.filters;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class MetricsWebFilter implements WebFilter {

    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsWebFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long start = System.currentTimeMillis();
        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - start;
            Timer.builder("http.server.requests")
                    .tag("method", exchange.getRequest().getMethod().name())
                    .tag("uri", exchange.getRequest().getPath().value())
                    .tag("status", String.valueOf(exchange.getResponse().getStatusCode()))
                    .register(meterRegistry)
                    .record(Duration.ofMillis(duration));
        });
    }
}
