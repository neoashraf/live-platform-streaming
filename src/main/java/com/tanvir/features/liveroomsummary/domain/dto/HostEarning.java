package com.tanvir.features.liveroomsummary.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HostEarning {
    private String userId;
    private long totalDuration;
    private String totalDurationString;
    private double totalGems;
    private String totalGemsValue;
    private int totalDayTimeCount;

}
