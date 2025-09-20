package com.tanvir.features.liveroom.application.port.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomHeartBeatResponse {
    private String message;
    private LiveRoomHeartBeatDto data;
    private Integer count;
    private boolean error;
}
