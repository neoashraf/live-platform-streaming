package com.tanvir.features.liveroom.application.port.in.dto.response;

import com.tanvir.features.liveroom.domain.valueobject.Fan;
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
public class LiveRoomResponse {
    private String id;
    private String userId;
    private String country;
    private String profilePicture;
    private String name;
    private String welcomeNote;
    private Integer starCount;
    private Long gemsCount;
    private String type;
    private String tag;
    private Integer fansCount;
    private Long beansCount;
    private String isLive;
    private Integer popularityLevel;
    private Integer userLevel;
    private List<Fan> fans;
    private Long duration;
    private Integer levelCompletionPercentage;

    private String createdBy;
    private LocalDateTime createdOn;
    private String endedBy;
    private LocalDateTime endedOn;
}
