package com.tanvir.features.liveroomsummary.domain;

import com.tanvir.features.liveroomsummary.adapter.out.persistence.entity.LiveRoomSummaryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomSummary {
    private String id;
    private String userId;
    private String maxId;
    private String agencyId;
    private String date;
    private int totalDuration;
    private String totalDurationString;
    private int totalSessions;
    private String dayTime;
    private double totalBonus;
    private double hostDailyGems;
    private int totalLiveDays;
    private List<LiveRoomSummaryEntity.SessionDetail> sessionDetails;

}
