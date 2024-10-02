package com.tanvir.features.liveroom.domain;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.gifttransaction.domain.valueobjects.HostDailyStarProgress;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.Viewer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.swing.text.View;
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
    private List<String> kickedOutUserIds;
    private List<String> viewerIds;
    private long viewerCount;
    private double hostDailyGems;

    private Viewer viewer;
    private Announcement announcement;
    private long durationInSeconds;
    private HostDailyStarProgress hostDailyStarProgress;


    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
