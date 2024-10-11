package com.tanvir.features.liveroom.domain;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.DailyStarProgress;
import com.tanvir.features.liveroom.domain.valueobject.JoinRequests;
import com.tanvir.features.liveroom.domain.valueobject.Viewer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoom {
    private String id;
    private LocalDateTime createdOn;
    private LocalDateTime endedOn;
    private String thumbnailId;
    private String thumbnailUrl;

    private String title;
    private String description;
    private List<String> tags;
    private String type;
    private String status;
    private String country;
    private String hostId;
    private String userId;
    private String hostMaxId;
    private List<String> kickedOutUserIds;
    private List<String> viewerIds;
    private long viewerCount;
    private long totalViewerCount;
    private double hostDailyGems;

    private Viewer viewer;
    private Announcement announcement;
    private long durationInSeconds;
    private DailyStarProgress dailyStarProgress;
    private String enableJoin;
    private List<JoinRequests> joinRequests;
    private Announcement welcomeAnnouncement;


    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
