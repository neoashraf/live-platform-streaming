package com.tanvir.core.filters;

import lombok.Getter;

@Getter
public enum MDCKeys {
    TRACE_ID("Trace-ID"),
    METHOD("Method"),
    URI("Uri"),
    ;

    private final String value;

    MDCKeys(String value) {
        this.value = value;
    }
}
