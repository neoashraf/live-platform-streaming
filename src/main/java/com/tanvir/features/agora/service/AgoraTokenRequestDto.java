package com.tanvir.features.agora.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgoraTokenRequestDto {
    private String appId;
    private String appCertificate;
    private String channelName;
}
