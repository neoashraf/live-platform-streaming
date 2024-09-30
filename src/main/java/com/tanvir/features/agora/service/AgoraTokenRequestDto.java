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
    private String role;

    private String account;
    private int uid;
    private int tokenExpirationInSeconds;
    private int privilegeExpirationInSeconds;
    private int joinChannelPrivilegeExpireInSeconds;
    private int pubAudioPrivilegeExpireInSeconds;
    private int pubVideoPrivilegeExpireInSeconds;
    private int pubDataStreamPrivilegeExpireInSeconds;

    private String tokenType;
}
