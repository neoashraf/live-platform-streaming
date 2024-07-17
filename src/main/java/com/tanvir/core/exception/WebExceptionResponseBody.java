package com.tanvir.core.exception;

import com.tanvir.core.util.CommonFunctions;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebExceptionResponseBody {
    private LocalDateTime timestamp;
    private String path;
    private int status;
    private String error;
    private String message;
    private String requestId;
    private String traceId;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
