package com.tanvir.features.liveroom.application.port.in.dto.request;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomRequestDto {
    private String keycloakId;
    private String userId;
    private String profilePicture;
    private String title;
    private String welcomeNote;
    private String type;
    private List<String> tags;
    private String description;
    private String thumbnailId;
    private String thumbnailUrl;
    private String tokenType;
    private String audioSkinId;
    private Integer audioSeatNumber;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
