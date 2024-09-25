package com.tanvir.features.host.application.port.in.dto.response;

import com.tanvir.features.host.domain.Host;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HostResponseDto {
    private String message;
    private List<Host> data;
    private Integer count;
    private boolean error;
}
