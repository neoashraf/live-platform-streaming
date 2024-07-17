package com.tanvir.core.exception;

import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@Configuration
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        WebExceptionResponseBody webExceptionResponseBody = WebExceptionResponseBody.builder()
            .path(exchange.getRequest().getPath().value())
            .requestId(exchange.getRequest().getId())
            .timestamp(LocalDateTime.now())
            .message(ex.getMessage())
            .traceId(Objects.requireNonNull(exchange.getRequest().getHeaders().get("Trace-Id")).get(0))
            .build();
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (ex instanceof ExceptionHandlerUtil) {
            exchange.getResponse().setStatusCode(((ExceptionHandlerUtil) ex).getCode());
            webExceptionResponseBody.setStatus(((ExceptionHandlerUtil) ex).getCode().value());
            webExceptionResponseBody.setError(((ExceptionHandlerUtil) ex).getCode().getReasonPhrase());
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            webExceptionResponseBody.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            webExceptionResponseBody.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(webExceptionResponseBody.toString().getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
