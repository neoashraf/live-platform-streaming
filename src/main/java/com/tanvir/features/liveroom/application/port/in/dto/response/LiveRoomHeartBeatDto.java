package com.tanvir.features.liveroom.application.port.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomHeartBeatDto {
    private String liveRoomId;
    private Instant time;
}
