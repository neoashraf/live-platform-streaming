package com.tanvir.features.liveroom.domain.valueobject;

import com.tanvir.features.liveroom.domain.LiveRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LiveRoomContentUrl {
    private LiveRoom liveRoom;
    private String contentUrl;
}
