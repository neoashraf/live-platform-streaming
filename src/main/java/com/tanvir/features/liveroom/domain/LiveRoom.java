package com.tanvir.features.liveroom.domain;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.DailyStarProgress;
import com.tanvir.features.liveroom.domain.valueobject.JoinRequests;
import com.tanvir.features.liveroom.domain.valueobject.Viewer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoom {
    private String id;
    private Instant createdOn;
    private Instant endedOn;
    private String thumbnailId;
    private String thumbnailUrl;

    private int month;
    private int year;
    private Double giftReceivedAmount;

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
    private String hostGender;

    private Viewer viewer;
    private Announcement announcement;
    private long durationInSeconds;
    private DailyStarProgress dailyStarProgress;
    private String enableJoin;
    private String enableAutoJoin;
    private List<JoinRequests> joinRequests;
    private Announcement welcomeAnnouncement;

    private double hostTotalGems;
    private String hostGemsValue;

    private int maxAudioParticipants;
    private List<Viewer> audioParticipants;
    private Summary summary;
    private String audioSkinId;
    private String audioSkinUrl;
    private Integer audioSeatNumber;
    private Instant lastSeen;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
