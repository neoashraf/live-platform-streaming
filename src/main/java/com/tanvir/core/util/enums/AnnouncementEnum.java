package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum AnnouncementEnum {
    ANNOUNCEMENT_MESSAGE_JOIN_CASUAL("{{mentionedUser}} has entered the room"),
    ANNOUNCEMENT_MESSAGE_JOIN_RIDE_ENTRY("{{mentionedUser}} has entered with a {{resource}}"),
    ANNOUNCEMENT_MESSAGE_JOIN_ENTRY_CARD_ENTRY("{{mentionedUser}} has entered with {{resource}}"),
    ANNOUNCEMENT_MESSAGE_KICK("{{publisher}} kicked {{mentionedUser}}"),

    ANNOUNCEMENT_TYPE_JOIN_CASUAL("ENTRY"),
    ANNOUNCEMENT_TYPE_JOIN_RIDE("RIDE_ENTRY"),
    ANNOUNCEMENT_TYPE_JOIN_ENTRY_CARD("ENTRY_CARD_ENTRY"),

    ANNOUNCEMENT_TYPE_KICK("KICK"),
    ;
    private final String value;

    AnnouncementEnum(String value) {
        this.value = value;
    }
}
