package com.tanvir.features.liveroom.application.port.in.dto.request;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinCallRequestUpdateDto {
    private String liveRoomId;
    private String keycloakId;
    private String cameraOn;
    private String cameraView;
    private String micOn;
    private String requestId;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
