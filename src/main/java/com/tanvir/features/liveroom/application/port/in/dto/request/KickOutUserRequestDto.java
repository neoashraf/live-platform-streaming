package com.tanvir.features.liveroom.application.port.in.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KickOutUserRequestDto {
    // body
    private String userId;

    // path
    private String liveRoomId;

    // param
    private String keycloakId;
}
