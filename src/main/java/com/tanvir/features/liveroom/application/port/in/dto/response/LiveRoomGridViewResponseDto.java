package com.tanvir.features.liveroom.application.port.in.dto.response;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomGridViewResponseDto {
    private String userMessage;
    private List<LiveRoomResponse> data;
    private Integer count;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
