package com.tanvir.features.liveroom.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveStreamInfo {
    private String type;
    private String tag;
    private Integer starCount;
    private Long gemsCount;
    private Long beansCount;
    private Long durationInSeconds;
    private String durationString;
}
