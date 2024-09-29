package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum TransactionTypeEnum {
    TRANSACTION_TYPE_SENT("SENT"),
    TRANSACTION_TYPE_RECEIVED("RECEIVED");

    private final String value;

    TransactionTypeEnum(String value) {
        this.value = value;
    }
}
