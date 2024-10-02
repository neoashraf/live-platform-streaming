package com.tanvir.features.commonbusiness;

import com.tanvir.core.util.enums.AnnouncementEnum;

import java.util.Map;

public class CommonBusiness {

    public static String formatToK(double value) {
        if (value >= 1000) {
            double kValue = value / 1000;
            if (kValue == (long) kValue) {
                return String.format("%dK", (long) kValue);
            } else {
                return String.format("%.1fK", kValue);
            }
        }
        return String.valueOf((long) value);
    }

    public static String convertToShortName(Double value) {
        if (value >= 1e9) {
            return String.format("%.1fB", value / 1e9);
        }
        else if (value >= 1e6) {
            return String.format("%.1fM", value / 1e6);
        }
        else if (value >= 1e3) {
            return String.format("%.1fK", value / 1e3);
        }
        else {
            return String.format("%.1f", value);
        }
    }

    public static String getAnnouncementMessage(String announcementType) {
        Map<String, String> announcementMessage = Map.of(
                AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_CASUAL.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_JOIN_CASUAL.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_RIDE.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_JOIN_RIDE_ENTRY.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_ENTRY_CARD.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_JOIN_ENTRY_CARD_ENTRY.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_KICK.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_KICK.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_COMMENT.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_COMMENT.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_GIFT.getValue()
        );

        return announcementMessage.get(announcementType);
    }

}
