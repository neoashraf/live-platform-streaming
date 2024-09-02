package com.tanvir.features.liveroom.application.port.in.dto.request;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.liveroom.domain.valueobject.Fan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomEntryLeaveRequestDto {
    private String keycloakId;
    private String liveRoomId;
    private Fan fan;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
