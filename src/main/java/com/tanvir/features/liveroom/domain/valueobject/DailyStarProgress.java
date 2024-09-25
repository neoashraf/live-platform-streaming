package com.tanvir.features.liveroom.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyStarProgress {
    private int starLevel;
    private int nextStarLevel;
    private long totalGiftsReceived;
    private long trailingByNextLevelGiftAmount;
}
