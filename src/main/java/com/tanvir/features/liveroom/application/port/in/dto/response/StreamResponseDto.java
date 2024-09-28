package com.tanvir.features.liveroom.application.port.in.dto.response;

import com.tanvir.features.liveroom.domain.valueobject.RoomDataDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamResponseDto {
    private String message;
    private RoomDataDto data;
    private int count;
    private boolean error;
}
