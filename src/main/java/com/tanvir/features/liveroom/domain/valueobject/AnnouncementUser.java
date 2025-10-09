package com.tanvir.features.liveroom.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementUser {
    private String userId;
    private String maxId;
    private String name;
    private String levelUrl;
    private String profileFrameId;
    private String profileFrameUrl;
    private String profileImageUrl;
    private String profileImageId;

}
