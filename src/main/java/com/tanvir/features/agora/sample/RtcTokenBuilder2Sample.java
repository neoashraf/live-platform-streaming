package com.tanvir.features.agora.sample;

import com.tanvir.features.agora.media.RtcTokenBuilder2;
import com.tanvir.features.agora.media.RtcTokenBuilder2.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RtcTokenBuilder2Sample {

    /*@Value("${agora.app.id}")
    private String appId;*/
    private static String appId = "0f27c7a324a04bf3be618afe43a4322d";

    /*@Value("${agora.app.certificate}")
    private String appCertificate;*/
    private static String appCertificate = "47c61e58b3f84323ac6bce5d3738d567";

    private static String channelName = "max-live-dev-test-channel";
    private static String account = "2082341273";
    private static int uid = 2082341273;
    private static int tokenExpirationInSeconds = 3600;
    private static int privilegeExpirationInSeconds = 3600;
    private static int joinChannelPrivilegeExpireInSeconds = 3600;
    private static int pubAudioPrivilegeExpireInSeconds = 3600;
    private static int pubVideoPrivilegeExpireInSeconds = 3600;
    private static int pubDataStreamPrivilegeExpireInSeconds = 3600;

    public void run() {
        System.out.printf("App Id: %s\n", appId);
        System.out.printf("App Certificate: %s\n", appCertificate);
        if (appId == null || appId.isEmpty() || appCertificate == null || appCertificate.isEmpty()) {
            System.out.printf("Need to set environment variable AGORA_APP_ID and AGORA_APP_CERTIFICATE\n");
            return;
        }

        RtcTokenBuilder2 token = new RtcTokenBuilder2();
        String result =
                token.buildTokenWithUid(appId, appCertificate, channelName, uid, Role.ROLE_PUBLISHER, tokenExpirationInSeconds, privilegeExpirationInSeconds);
        System.out.printf("Token with uid: %s\n", result);

        result = token.buildTokenWithUserAccount(appId, appCertificate, channelName, account, Role.ROLE_PUBLISHER, tokenExpirationInSeconds,
                privilegeExpirationInSeconds);
        System.out.printf("Token with account: %s\n", result);

        result = token.buildTokenWithUid(appId, appCertificate, channelName, uid, tokenExpirationInSeconds, joinChannelPrivilegeExpireInSeconds,
                pubAudioPrivilegeExpireInSeconds, pubVideoPrivilegeExpireInSeconds, pubDataStreamPrivilegeExpireInSeconds);
        System.out.printf("Token with uid and privilege: %s\n", result);

        result = token.buildTokenWithUserAccount(appId, appCertificate, channelName, account, tokenExpirationInSeconds, joinChannelPrivilegeExpireInSeconds,
                pubAudioPrivilegeExpireInSeconds, pubVideoPrivilegeExpireInSeconds, pubDataStreamPrivilegeExpireInSeconds);
        System.out.printf("Token with account and privilege: %s\n", result);

        result = token.buildTokenWithRtm(appId, appCertificate, channelName, account, Role.ROLE_PUBLISHER, tokenExpirationInSeconds,
                privilegeExpirationInSeconds);
        System.out.printf("Token with RTM: %s\n", result);
    }

    public static void main(String[] args) {
        RtcTokenBuilder2Sample sample = new RtcTokenBuilder2Sample();
        sample.run();
    }
}
