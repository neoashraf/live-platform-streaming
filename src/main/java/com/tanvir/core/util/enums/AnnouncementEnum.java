package com.tanvir.core.util.enums;

import lombok.Getter;

@Getter
public enum AnnouncementEnum {
    ANNOUNCEMENT_MESSAGE_JOIN_CASUAL("{{mentionedUser}} has entered the room"),
    ANNOUNCEMENT_MESSAGE_JOIN_RIDE_ENTRY("{{mentionedUser}} has entered with a {{resource}}"),
    ANNOUNCEMENT_MESSAGE_JOIN_ENTRY_CARD_ENTRY("{{mentionedUser}} has entered with {{resource}}"),
    ANNOUNCEMENT_MESSAGE_KICK("{{publisher}} kicked {{mentionedUser}}"),
    ANNOUNCEMENT_MESSAGE_COMMENT("{{publisher}}: "),
    ANNOUNCEMENT_MESSAGE_GIFT("{{mentionedUser}} sent {{gift.quantity}} x {{gift.resource}}"),

    ANNOUNCEMENT_MESSAGE_GIFT_UPDATE("{{mentionedUser}} sent {{receiverUser}} {{gift.quantity}} x {{gift.resource}}"),

    ANNOUNCEMENT_MESSAGE_WELCOME("{{publisher}}: {{mentionedUser}} "),

    ANNOUNCEMENT_TYPE_JOIN_CASUAL("ENTRY"),
    ANNOUNCEMENT_TYPE_JOIN_RIDE("RIDE_ENTRY"),
    ANNOUNCEMENT_TYPE_JOIN_ENTRY_CARD("ENTRY_CARD_ENTRY"),

    ANNOUNCEMENT_TYPE_KICK("KICK"),
    ANNOUNCEMENT_TYPE_COMMENT("COMMENT"),
    ANNOUNCEMENT_TYPE_GIFT("GIFT"),

    ANNOUNCEMENT_TYPE_GIFT_UPDATE("GIFT_UPDATE")
    ;
    private final String value;

    AnnouncementEnum(String value) {
        this.value = value;
    }
}
