package com.tanvir.features.liveroom.application.port.in.dto.request;

import com.tanvir.core.util.CommonFunctions;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeatNumberDto {
    private boolean availableStatus;
    private String userId;
    private String joinReqId;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
