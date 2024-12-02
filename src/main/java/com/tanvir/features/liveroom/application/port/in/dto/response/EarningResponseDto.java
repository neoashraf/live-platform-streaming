package com.tanvir.features.liveroom.application.port.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningResponseDto {
    private String message;
    private EarningModel data;
    private int count;
    private boolean error;

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EarningModel {
        private String month;
        private int year;
        private double gems;
        private String gemsString;
        private String duration;
        private String durationString;
        private int validDays;
        private int bonus;
        private String bonusString;
    }
}
