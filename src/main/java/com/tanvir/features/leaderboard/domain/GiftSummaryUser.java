package com.tanvir.features.leaderboard.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GiftSummaryUser {
    private String userId;
    private String displayName;
    private String profileImageUrl;
    private String profileFrameUrl;
    private String levelUrl;
    private Integer userLevel;
    private Double beansSent;
    private Double beansReceived;
    private String beansReceivedValue;
    private String agencyId;
}
