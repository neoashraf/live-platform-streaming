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
public class LiveRoomRequestDto {
    private String keycloakId;
    private String userId;
    private String profilePicture;
    private String name;
    private String welcomeNote;
    private String type;
    private String tag;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
