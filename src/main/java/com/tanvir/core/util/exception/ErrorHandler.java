package com.tanvir.core.util.exception;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@UtilityClass
public class ErrorHandler {

    public Mono<ServerResponse> buildErrorResponseForBusiness(ExceptionHandlerUtil ex, ServerRequest request) {
        return ServerResponse.status(ex.getCode()).bodyValue(new ErrorBody(ex, request.path()));
    }

    public Mono<ServerResponse> buildErrorResponseForUncaught(Throwable ex, ServerRequest request) {
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(new ErrorBody(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ex.getMessage(), request.path()));
    }

}
