package com.tanvir.features.liveroom.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomDataDto {
    private String id;
    private String userId;
    private String hostMaxId;
    private String viewerMaxId;
    private Instant joinedOn;
    private Instant leftOn;
    private Instant kickedOn;
    private Instant endedOn;
    private Instant commentedOn;
    private Announcement announcement;
    private Announcement welcomeAnnouncement;
    private Long durationInSeconds;
    private String duration;
    private String agoraToken;

    // for create stream
    private String thumbnailId;
    private String thumbnailUrl;
    private String title;
    private String type;
    private List<String> tags;
    private String status;
    private long viewerCount;
    private Instant createdOn;
    private String country;
    private Viewer viewer;
    private double hostDailyGems;

    // for end stream
    private long totalViewerCount;

    private String enableJoin;
    private String roomId;
}
