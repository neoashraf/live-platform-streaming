package com.tanvir.features.liveroom.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Earning {
    private String month;
    private int year;
    private double gems;
    private String gemsString;
    private String duration;
    private String durationString;
    private int validDays;
    private double bonus;
    private String bonusString;
}
