package com.tanvir.features.level.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Level {
    private String id;
    private int level;
    private String levelBadgeId;
    private String levelBadgeUrl;
    private long nextLevelExpTargetValue;
    private String nextLevelExpTargetName;
}
