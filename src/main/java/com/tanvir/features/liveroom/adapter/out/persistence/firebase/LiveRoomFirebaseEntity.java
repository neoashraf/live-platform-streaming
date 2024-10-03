package com.tanvir.features.liveroom.adapter.out.persistence.firebase;

import com.tanvir.common.firebase.BaseFirebaseEntity;
import com.tanvir.features.liveroom.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LiveRoomFirebaseEntity implements BaseFirebaseEntity {
    private String id;
    private String thumbnailId;
    private String thumbnailUrl;
    private String title;
    private String description;
    private List<String> tags;
    private String type;
    private String status;
    private String country;
    private HostSummary host;
    private List<Viewer> viewers;
    private List<String> viewerIds;
    private long viewerCount;
    private List<Announcement> announcements;
    private DailyStarProgress dailyStarProgress;
//    private int elapsedSeconds;
//    private String formattedTime;
}
