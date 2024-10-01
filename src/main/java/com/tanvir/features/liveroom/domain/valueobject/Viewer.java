package com.tanvir.features.liveroom.domain.valueobject;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Viewer {
    private String userId;
    private String maxId;
    private String displayName;
    private String gender;
    private String profilePictureUrl;
    private String frameUrl;
    private int userLevel;
    private String levelBadgeUrl;
    private String rideId;
    private String entryCardId;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
