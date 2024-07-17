package com.tanvir.core.config;

import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.Map;
import java.util.Optional;

@Configuration
public class HooksConfig {

    public HooksConfig() {
        Hooks.onEachOperator(ApplicationContext.class.getName(), Operators.lift((scannable, coreSubscriber) -> new CoreSubscriber<Object>() {
            @Override
            public Context currentContext() {
                return coreSubscriber.currentContext();
            }

            @Override
            public void onSubscribe(Subscription s) {
                if (coreSubscriber.currentContext().hasKey("mdcContextMap")) {
                    Optional.of(coreSubscriber.currentContext().get("mdcContextMap"))
                            .ifPresent(contextMap -> MDC.setContextMap((Map<String, String>) contextMap));
                }
                coreSubscriber.onSubscribe(s);
            }

            @Override
            public void onNext(Object o) {
                coreSubscriber.onNext(o);
            }

            @Override
            public void onError(Throwable t) {
                coreSubscriber.onError(t);
            }

            @Override
            public void onComplete() {
                coreSubscriber.onComplete();
            }
        }));
    }
}
