package com.tanvir.features.liveroom.application.port.in.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinSettingsRequestDto {
    private String mode;
    private String status ;
    private String liveRoomId;
    private String keycloakId;
    private String mediaType;
}
