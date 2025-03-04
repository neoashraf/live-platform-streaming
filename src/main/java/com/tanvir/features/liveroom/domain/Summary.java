package com.tanvir.features.liveroom.domain;

import com.tanvir.features.liveroom.application.service.LiveRoomService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Summary {
    private String endedOn;
    private String endedBy;
    private long durationInSeconds;
    private String duration;
    private long viewerCount;
    private double  hostDailyGems;
    private int  maxAudioParticipants;
    private long totalViewerCount;
    private double giftReceivedAmount;
    private String giftReceivedAmountString ;
}
