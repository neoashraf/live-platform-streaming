package com.tanvir.core.util.exception;

import com.tanvir.core.util.helper.GenericResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
public class GenericErrorHandlerUtil {
    public static Mono<ServerResponse> buildGenericErrorResponse(Throwable e, ServerRequest serverRequest) {
        HttpStatus status = e instanceof ExceptionHandlerUtil ? ((ExceptionHandlerUtil) e).code : HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = e.getMessage();
        String path = serverRequest.path();
        String requestId = serverRequest.exchange().getRequest().getId(); // Example logic for request ID
        String traceId = "Trace-ID"; // Implement logic to generate/fetch trace ID

        GenericResponseDto errorResponse = GenericResponseDto.builder()
                .message(errorMessage)
                .error(true)
                .build();

        log.error("error response built : {}", errorResponse);

        return ServerResponse
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }
}
