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
    private int totalBonus;
    private List<LiveRoomSummaryEntity.SessionDetail> sessionDetails;

    public static class SessionDetail {
        private String liveRoomId;
        private int duration;
    }
}
