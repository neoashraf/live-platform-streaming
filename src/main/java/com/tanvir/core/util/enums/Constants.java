package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum Constants {
    STATUS_YES("Yes"),
    STATUS_NO("No"),
    LIVE_ROOM_TYPE_VIDEO("Video"),
    LIVE_ROOM_TYPE_AUDIO("Audio"),
    TAB_POPULAR("Popular"),
    TAB_FRESHER("Fresher"),
    TAB_PARTY("Party"),

    STATUS_LIVE("Live"),
    STATUS_OFFLINE("Offline")
    ;
    private final String value;

    Constants(String value) {
        this.value = value;
    }
}
