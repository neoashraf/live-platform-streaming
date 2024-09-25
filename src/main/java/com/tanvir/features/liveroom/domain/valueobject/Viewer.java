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
    private String name;
    private Integer userLevel;
    private String profilePicture;
    private String entryBanner;
    private String frame;
    private String ride;
    private LocalDateTime entryTime;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
