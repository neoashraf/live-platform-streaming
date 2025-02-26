package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum Constants {

    HOST_TYPE("host"),
    STATUS_YES("Yes"),
    STATUS_NO("No"),
    LIVE_ROOM_TYPE_VIDEO("Video"),
    LIVE_ROOM_TYPE_AUDIO("Audio"),
    TAB_POPULAR("POPULAR"),
    TAB_FRESHER("FRESHER"),
    TAB_PARTY("PARTY"),

    STATUS_LIVE("Live"),
    STATUS_OFFLINE("Offline"),
    STATUS_PENDING("Pending"),
    STATUS_APPROVED("Approved"),
    STATUS_REJECTED("Rejected"),
    STATUS_CLOSED("Closed"),
    STATUS_DECLINE("Declined"),
    STATUS_STARTED("Started"),

    CAMERA_VIEW_FRONT("Front"),
    CAMERA_VIEW_BACK("Back"),

    USER_TYPE_SENDER("Sender"),
    USER_TYPE_RECEIVER("Receiver"),
    ;
    private final String value;

    Constants(String value) {
        this.value = value;
    }
}
