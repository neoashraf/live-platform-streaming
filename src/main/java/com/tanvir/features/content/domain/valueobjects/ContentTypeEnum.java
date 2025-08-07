package com.tanvir.features.content.domain.valueobjects;

import lombok.Getter;

@Getter
public enum ContentTypeEnum {
    RIDE_ENTRY("RIDE_ENTRY"),
    FRAME("FRAME"),
    ENTRY_CARD("ENTRY_CARD"),
    AUDIO_SKIN("AUDIO_SKIN");

    private final String value;

    ContentTypeEnum(String value) {
        this.value = value;
    }
}

