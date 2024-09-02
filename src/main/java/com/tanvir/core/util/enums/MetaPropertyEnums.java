package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum MetaPropertyEnums {
    PASSWORD_META_PROPERTY("Password Meta-Property"),
    INDEX_META_PROPERTY("Index Meta-Property"),
    ;

    private final String value;

    MetaPropertyEnums(String value) {
        this.value = value;
    }
}
