package com.tanvir.core.config;

import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

//@AutoConfiguration(before = MicrometerTracingAutoConfiguration.class)
@ConditionalOnClass({ Tracer.class, BraveTracer.class })
//@EnableConfigurationProperties(TracingProperties.class)
public class TracerConfig {

    private static final BraveBaggageManager BRAVE_BAGGAGE_MANAGER = new BraveBaggageManager();

    /*@Bean
    Tracer tracer() {
        return BraveTracer
    }*/

/*    @Bean
    ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }*/

    @Bean
    @ConditionalOnMissingBean
    public brave.Tracer braveTracer(Tracing tracing) {

//        Tracer
        return tracing.tracer();
    }

    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    BraveTracer braveTracerBridge(brave.Tracer tracer, CurrentTraceContext currentTraceContext) {
        return new BraveTracer(tracer, new BraveCurrentTraceContext(currentTraceContext), BRAVE_BAGGAGE_MANAGER);
    }
}
