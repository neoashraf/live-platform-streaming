package com.tanvir.features.agora.service;

import com.tanvir.features.agora.media.RtcTokenBuilder2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AgoraService {

    private static String account = "2082341273";
    private static int uid = (int) (Math.random() * 1000000);
    private static int tokenExpirationInSeconds = 3600;
    private static int privilegeExpirationInSeconds = 3600;
    private static int joinChannelPrivilegeExpireInSeconds = 3600;
    private static int pubAudioPrivilegeExpireInSeconds = 3600;
    private static int pubVideoPrivilegeExpireInSeconds = 3600;
    private static int pubDataStreamPrivilegeExpireInSeconds = 3600;

    public Mono<AgoraTokenResponseDto> generateToken(AgoraTokenRequestDto requestDto) {
//        log.info("Generating token for request: {}", requestDto);


        RtcTokenBuilder2 token = new RtcTokenBuilder2();
        AgoraToken agoraToken = new AgoraToken();

        agoraToken.setTokenWithUid(token.buildTokenWithUid(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), uid, RtcTokenBuilder2.Role.ROLE_PUBLISHER, tokenExpirationInSeconds, privilegeExpirationInSeconds));

        agoraToken.setTokenWithUserAccount(token.buildTokenWithUserAccount(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), account, RtcTokenBuilder2.Role.ROLE_PUBLISHER, tokenExpirationInSeconds,
                privilegeExpirationInSeconds));

        agoraToken.setTokenWithUidAndPrivilege(token.buildTokenWithUid(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), uid, tokenExpirationInSeconds, joinChannelPrivilegeExpireInSeconds,
                pubAudioPrivilegeExpireInSeconds, pubVideoPrivilegeExpireInSeconds, pubDataStreamPrivilegeExpireInSeconds));

        agoraToken.setTokenWithAccountAndPrivilege(token.buildTokenWithUserAccount(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), account, tokenExpirationInSeconds, joinChannelPrivilegeExpireInSeconds,
                pubAudioPrivilegeExpireInSeconds, pubVideoPrivilegeExpireInSeconds, pubDataStreamPrivilegeExpireInSeconds));

        agoraToken.setTokenWithRtm(token.buildTokenWithRtm(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), account, RtcTokenBuilder2.Role.ROLE_PUBLISHER, tokenExpirationInSeconds,
                privilegeExpirationInSeconds));

        return Mono.just(AgoraTokenResponseDto.builder()
                .message("Token generated successfully")
                .data(List.of(agoraToken))
                .count(1)
                .build());
    }
}
