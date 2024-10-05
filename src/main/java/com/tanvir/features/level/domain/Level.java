package com.tanvir.features.level.domain;

import com.tanvir.features.level.domain.valueobjects.ResourceFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Level {
    private String id;
    private int level;
    private List<ResourceFormat> resourceFormats;
    private long nextLevelExpTargetValue;
    private String nextLevelExpTargetName;
}
