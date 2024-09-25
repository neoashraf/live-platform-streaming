package com.tanvir.features.liveroom.adapter.out.persistence.firebase;

import com.tanvir.common.firebase.BaseFirebaseEntity;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.Fan;
import com.tanvir.features.liveroom.domain.valueobject.HostSummary;
import com.tanvir.features.liveroom.domain.valueobject.Viewer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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
    private long viewerCount;
    private List<Announcement> announcements;
}
