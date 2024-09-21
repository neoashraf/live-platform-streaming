package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum UserTypeEnum {
    USER_TYPE_USER("user"),
    USER_TYPE_MAX_USER("max_user"),
    USER_TYPE_MASTER_PORTAL_ADMIN("master_portal_admin"),
    USER_TYPE_RESELLER("reseller"),
    USER_TYPE_COUNTRY_ADMIN("country_admin"),
    USER_TYPE_HOST("host"),
    USER_TYPE_AGENCY("agency"),
    USER_TYPE_OWNER("owner"),

    // ID Related
    USER_TYPE_REFERRAL_ID("referral_id"),
    USER_TYPE_BEAN_TRANSACTION_ID("bean_transaction_id"),
    ;
    private final String value;

    UserTypeEnum(String value) {
        this.value = value;
    }
}
