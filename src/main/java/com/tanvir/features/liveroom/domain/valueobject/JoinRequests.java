package com.tanvir.features.liveroom.domain.valueobject;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinRequests {
    private String requestId;
    private String userId;
    private String maxId;
    private String displayName;
    private String cameraOn;
    private String cameraView;
    private String micOn;
    private String profileImageUrl;
    private String profileLevelUrl;
    private String profileFrameId;
    private String profileFrameUrl;
    private String status;
    private String reason;
    private String gender;
    @Builder.Default
    private double giftsReceivedInThisSession=0.0;
    private String giftsReceivedInThisSessionString;
    private int seatIndex;
    private Integer availableSeats;
    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
