package com.tanvir.features.liveroom.adapter.out.persistence.firebase;

import com.tanvir.common.firebase.BaseFirebaseEntity;
import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.liveroom.domain.valueobject.Fan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LiveRoomFirebaseEntity implements BaseFirebaseEntity {

    private String id;
    private String userId;
    private String country;
    private String profilePicture; // profilePictureUrl
    private String name;
    private String welcomeNote;
    private Integer starCount;
    private Long gemsCount;
    private Long beansCount;
    private String type;
    private String tag; // array
    private Integer fansCount;
    private String isLive;
    private Integer popularityLevel; // no need
    private Integer userLevel;
    private Map<String, Fan> fans;
    private List<String> kickedOutUsers;
    private Long duration;
    private Integer levelCompletionPercentage; // no need (only needed in response)

    private String createdBy; // no need
//    private LocalDateTime createdOn;
    private String endedBy; // no need
//    private LocalDateTime endedOn;

    private String maxId;
    private String gender;
    private String profilePictureUrl;
    private long dailyReceivedGems;

}
