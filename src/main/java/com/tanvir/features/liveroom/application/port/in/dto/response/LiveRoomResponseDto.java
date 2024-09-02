package com.tanvir.features.liveroom.application.port.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomResponseDto {
    private String userMessage;
    private LiveRoomResponse data;
}
