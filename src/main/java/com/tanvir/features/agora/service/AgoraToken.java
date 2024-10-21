package com.tanvir.features.agora.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgoraToken {
    private String tokenWithUid;
    private String tokenWithUserAccount;
    private String tokenWithUidAndPrivilege;
    private String tokenWithAccountAndPrivilege;
    private String tokenWithRtm;

    private String agoraToken;
}
