package com.tanvir.features.gifttransaction.domain.valueobjects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HostDailyStarProgress {
    private double currentGems;
    private int currentStar;
    private double nextStarGems;
    private double gemsNeededForNextStar;
}
