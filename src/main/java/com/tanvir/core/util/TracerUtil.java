package com.tanvir.core.util;

import brave.Tracer;
//import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class TracerUtil {

    @Autowired
    private Tracer tracer;

    public String getCurrentTraceId() {
        return "Trace-ID";
//        return String.valueOf(tracer.currentSpan().context().traceId());
    }
}
