package com.tanvir.features.liveroomsummary.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "liveroom_summary")
public class LiveRoomSummaryEntity {

    @Id
    private String id;
    private String userId;
    private String maxId;
    private String agencyId;
    private String date;
    private long totalDuration;
    private String totalDurationString;
    private int totalSessions;
    private String dayTime;
    private double totalBonus;
    private double totalGiftReceivedAmount;
    private double hostDailyGems;
    private int month;
    private int year;
    private List<SessionDetail> sessionDetails;
    private int totalLiveDays;
    private String lastGemsAwardedDate;
    private String lastDayCountedDate;

    private long totalVideoDuration;
    private String totalVideoDurationString;
    private long totalAudioDuration;
    private String totalAudioDurationString;

    private Instant createdOn;
    private Instant updatedOn;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SessionDetail {
        private Instant createdOn;
        private String liveRoomId;
        private long duration;
        private double bonus;
        private double hostDailyGems;
        private double giftReceivedAmount;
        private String sessionType;
    }
}
