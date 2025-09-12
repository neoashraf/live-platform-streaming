package com.tanvir.features.liveroom.application.port.in.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinPermissionRequestDTO {
    private String liveRoomId;
    private String keycloakId;
    private String enableAutoJoin;
    private String enableJoin;
}
