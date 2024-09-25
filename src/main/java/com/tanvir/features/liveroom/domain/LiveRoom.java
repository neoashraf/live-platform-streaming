package com.tanvir.features.liveroom.domain;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.liveroom.domain.valueobject.Fan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoom {
    private String id;
    private String userId;
    private String keycloakId;
    private String country;
    private String profilePicture;
    private String name;
    private String welcomeNote;
    private Integer starCount;
    private double gemsCount;
    private String type;
    private String tag;
    private Integer fansCount;
    private Long beansCount;
    private String status;
    private Integer popularityLevel;
    private Integer userLevel;
    private Map<String, Fan> fans;
    private List<String> kickedOutUsers;
    private Long duration;
    private Integer levelCompletionPercentage;

    private String createdBy;
    private LocalDateTime createdOn;
    private String endedBy;
    private LocalDateTime endedOn;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
