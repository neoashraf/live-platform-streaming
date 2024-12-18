package com.tanvir.features.liveroomsummary.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterSummary {
    private Instant createdOn;
    private String liveRoomId;
    private long duration;
    private double bonus;
    private double hostDailyGems;
    private double giftReceivedAmount;
    private String sessionType;
}
