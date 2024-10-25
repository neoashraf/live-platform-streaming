package com.tanvir.features.liveroomsummary.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HostEarningResponseDto {
    private String message;
    private HostEarning data;
    private int count;
    private boolean error;
}
