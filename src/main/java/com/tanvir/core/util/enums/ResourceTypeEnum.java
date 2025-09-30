package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum ResourceTypeEnum {
    RESOURCE_TYPE_IMAGE("IMAGE"),
    RESOURCE_TYPE_ANIMATION("SVGA"),
    RESOURCE_TYPE_MP3("MP3");

    private final String value;

    ResourceTypeEnum(String value) {
        this.value = value;
    }
}
