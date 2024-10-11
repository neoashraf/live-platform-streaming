package com.tanvir.features.liveroom.application.port.in.dto.response;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.liveroom.domain.valueobject.EnableJoinPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomJoinPermissionResponseDto {
    private String message;
    private EnableJoinPermission data;
    private int count;
    private boolean error;
    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
