package com.tanvir.core.util.exception;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Aspect
@Component
public class ExceptionHandlingAspect {
    @Around("execution(public reactor.core.publisher.Mono<org.springframework.web.reactive.function.server.ServerResponse> *(..))")
    public Mono<ServerResponse> handleErrors(ProceedingJoinPoint joinPoint) {
        try {
            // Proceed with the original method
            Mono<ServerResponse> result = (Mono<ServerResponse>) joinPoint.proceed();

            // Apply generic error handling
            return result.onErrorResume(e -> {
                ServerRequest serverRequest = (ServerRequest) joinPoint.getArgs()[0]; // Assuming ServerRequest is the first argument
                return GenericErrorHandlerUtil.buildGenericErrorResponse(e, serverRequest);
            });

        } catch (Throwable throwable) {
            // Handle any exception that occurs during method execution
            ServerRequest serverRequest = (ServerRequest) joinPoint.getArgs()[0];
            return GenericErrorHandlerUtil.buildGenericErrorResponse(throwable, serverRequest);
        }
    }
}
