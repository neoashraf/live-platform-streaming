package com.tanvir.features.liveroom.application.port.in.dto.request;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.liveroom.domain.valueobject.Viewer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomViewerRequestDto {
    private String keycloakId;
    private String liveRoomId;
//    private Fan fan;
    private Viewer viewer;
    private String comment;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
