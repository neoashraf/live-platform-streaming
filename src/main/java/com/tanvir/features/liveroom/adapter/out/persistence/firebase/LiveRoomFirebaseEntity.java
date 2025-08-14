package com.tanvir.features.liveroom.adapter.out.persistence.firebase;

import com.tanvir.common.firebase.BaseFirebaseEntity;
import com.tanvir.features.liveroom.application.port.in.dto.request.SeatNumberDto;
import com.tanvir.features.liveroom.domain.Summary;
import com.tanvir.features.liveroom.domain.valueobject.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
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

    private List<String> viewerIds;
    private long viewerCount;
    private List<Announcement> announcements;
    private String enableJoin;
    private String enableAutoJoin;
    private List<JoinRequests> joinRequests;
    private int maxAudioParticipants;
    private List<Viewer> audioParticipants;
    private List<String> kickedOutUserIds;
    private List<Boolean> seatAvailableStatus;
    private Map<String, SeatNumberDto> seatMap;

    private Summary summary;
    private String audioSkinId;
    private String audioSkinUrl;
    private Integer audioSeatNumber;
}
