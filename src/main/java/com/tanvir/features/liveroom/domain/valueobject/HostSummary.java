package com.tanvir.features.liveroom.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HostSummary {
    private String id;
    private String userId;
    private String hostMaxId;
    private String displayName;
    private String gender;
    private String profileImageId;
    private String profileImageUrl;
    private String profileFrameId;
    private String profileFrameUrl;
    private int userLevel;
    private String levelBadgeUrl;
    private double gems;
    private String gemsValue;
    private DailyStarProgress dailyStarProgress;
    private double giftsReceivedInThisSession;
    private String giftsReceivedInThisSessionString;
}
