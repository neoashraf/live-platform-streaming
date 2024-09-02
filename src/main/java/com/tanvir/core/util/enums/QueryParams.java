package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum QueryParams {
    ROLE("role"),
    COUNTRY("country"),
    GENDER("gender"),
    ACTIVE("active"),
    SEARCH_KEY("searchKey"),
    MAX_ID("maxId"),
    ID("id"),
    USER_ID("userId"),
    KEYCLOAK_ID("keycloakId"),
    USER_NAME("userName"),
    AGENCY_MAX_ID("agencyMaxId"),
    HOST_TYPE("hostType"),
    NID_FRONT_ID("nidFrontId"),
    NID_BACK_ID("nidBackId"),
    STATUS("status"),
    REFERRAL_ID("referralId"),
    SENDER_MAX_ID("senderMaxId"),
    SENDER_USER_TYPE("senderUserType"),
    RECEIVER_MAX_ID("receiverMaxId"),
    RECEIVER_USER_TYPE("receiverUserType"),
    BEANS("beans"),
    TRANSACTION_TYPE("transactionType"),
    TRANSACTION_ID("transactionId"),
    FROM_DATE("fromDate"),
    TO_DATE("toDate"),
    META_PROPERTY_NAME("metaPropertyName"),
    PROFILE_IMAGE_ID("profileImageId"),
    PROFILE_IMAGE_URL("profileImageUrl"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    DATE_OF_BIRTH("dateOfBirth"),

    ;
    private final String value;

    QueryParams(String value) {
        this.value = value;
    }
}
