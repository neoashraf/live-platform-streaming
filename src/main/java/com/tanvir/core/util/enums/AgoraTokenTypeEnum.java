package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum AgoraTokenTypeEnum {
   TOKEN_WITH_UID("TOKEN_WITH_UID"),
    TOKEN_WITH_USER_ACCOUNT("TOKEN_WITH_USER_ACCOUNT"),
    TOKEN_WITH_UID_AND_PRIVILEGE("TOKEN_WITH_UID_AND_PRIVILEGE"),
    TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE("TOKEN_WITH_USER_ACCOUNT_AND_PRIVILEGE"),
    TOKEN_WITH_RTM("TOKEN_WITH_RTM"),

    ROLE_PUBLISHER("PUBLISHER"),
    ROLE_SUBSCRIBER("SUBSCRIBER");


    private final String value;

    AgoraTokenTypeEnum(String value) {
        this.value = value;
    }
}
