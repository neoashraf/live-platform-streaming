package com.tanvir.features.agora.service;

import com.tanvir.core.util.enums.AgoraTokenTypeEnum;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.agora.media.RtcTokenBuilder2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AgoraService {

    /*private static String account = "2082341273";
    private static int uid = (int) (Math.random() * 1000000);
    private static int tokenExpirationInSeconds = 3600;
    private static int privilegeExpirationInSeconds = 3600;
    private static int joinChannelPrivilegeExpireInSeconds = 3600;
    private static int pubAudioPrivilegeExpireInSeconds = 3600;
    private static int pubVideoPrivilegeExpireInSeconds = 3600;
    private static int pubDataStreamPrivilegeExpireInSeconds = 3600;*/

    public Mono<AgoraTokenResponseDto> generateToken(AgoraTokenRequestDto requestDto) {
//        log.info("Generating token for request: {}", requestDto);


        RtcTokenBuilder2 token = new RtcTokenBuilder2();
        AgoraToken agoraToken = new AgoraToken();

        RtcTokenBuilder2.Role role;
        if (requestDto.getRole().equals(AgoraTokenTypeEnum.ROLE_PUBLISHER.getValue())) {
            role = RtcTokenBuilder2.Role.ROLE_PUBLISHER;
        } else if (requestDto.getRole().equals(AgoraTokenTypeEnum.ROLE_SUBSCRIBER.getValue())) {
            role = RtcTokenBuilder2.Role.ROLE_SUBSCRIBER;
        } else {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid role"));
        }

        List<String> validTokenTypes = List.of(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_UID_AND_PRIVILEGE.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE.getValue(), AgoraTokenTypeEnum.TOKEN_WITH_RTM.getValue());
        if (!validTokenTypes.contains(requestDto.getTokenType())) {
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid token type"));
        }


        if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_UID.getValue())) {
            agoraToken.setTokenWithUid(token.buildTokenWithUid(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), requestDto.getUid(), role, requestDto.getTokenExpirationInSeconds(), requestDto.getPrivilegeExpirationInSeconds()));
        }

        if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT.getValue())) {
            agoraToken.setTokenWithUserAccount(token.buildTokenWithUserAccount(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), requestDto.getAccount(), role, requestDto.getTokenExpirationInSeconds(),
                    requestDto.getPrivilegeExpirationInSeconds()));
        }
        if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_UID_AND_PRIVILEGE.getValue())) {
            agoraToken.setTokenWithUidAndPrivilege(token.buildTokenWithUid(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), requestDto.getUid(),requestDto.getTokenExpirationInSeconds(), requestDto.getJoinChannelPrivilegeExpireInSeconds(), requestDto.getPubAudioPrivilegeExpireInSeconds(), requestDto.getPubVideoPrivilegeExpireInSeconds(), requestDto.getPubDataStreamPrivilegeExpireInSeconds()));
        }

        if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE.getValue())) {
            agoraToken.setTokenWithAccountAndPrivilege(token.buildTokenWithUserAccount(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), requestDto.getAccount(), requestDto.getTokenExpirationInSeconds(), requestDto.getJoinChannelPrivilegeExpireInSeconds(), requestDto.getPubAudioPrivilegeExpireInSeconds(), requestDto.getPubVideoPrivilegeExpireInSeconds(), requestDto.getPubDataStreamPrivilegeExpireInSeconds()));
        }

        if (requestDto.getTokenType().equals(AgoraTokenTypeEnum.TOKEN_WITH_RTM.getValue())) {
            agoraToken.setTokenWithRtm(token.buildTokenWithRtm(requestDto.getAppId(), requestDto.getAppCertificate(), requestDto.getChannelName(), requestDto.getAccount(), role, requestDto.getTokenExpirationInSeconds(),
                    requestDto.getPrivilegeExpirationInSeconds()));
        }

        return Mono.just(AgoraTokenResponseDto.builder()
                .message("Token generated successfully")
                .data(List.of(agoraToken))
                .count(1)
                .build());
    }
}
