package com.tanvir.features.liveroom.application.port.in.dto.response;

import com.tanvir.features.liveroom.domain.valueobject.LiveStreamInfo;
import com.tanvir.features.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EndStreamResponseDto {
    private String userMessage;
    private User userInfo;
    private LiveStreamInfo liveStreamInfo;
}
