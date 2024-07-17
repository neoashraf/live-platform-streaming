package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum WithdrawType {
    REGULAR("Regular"),
    SPECIAL("Special"),
    ;
    private final String value;
    WithdrawType(String value){
        this.value = value;
    }
}
