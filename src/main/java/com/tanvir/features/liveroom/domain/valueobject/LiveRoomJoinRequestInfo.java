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
public class LiveRoomJoinRequestInfo {
    private String roomId;
    private String requestId;
    private String status;
    private String cameraOn;
    private String cameraView;
    private String micOn;
    private String reason;

    private String role;
    private String agoraToken;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
