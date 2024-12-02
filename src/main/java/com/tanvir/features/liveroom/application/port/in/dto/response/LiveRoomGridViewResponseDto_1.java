package com.tanvir.features.liveroom.application.port.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomGridViewResponseDto_1 {
    private String userMessage;
    private LiveRoomResponse data;
    private Integer count;
}
