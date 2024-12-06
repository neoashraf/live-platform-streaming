package com.tanvir.core.util;

public class FormatUtil {

    public static String formatGems(double bonus) {
        if (bonus >= 1_000_000) {
            return String.format("%.1fM", bonus / 1_000_000);
        } else if (bonus >= 1_000) {
            return String.format("%.1fk", bonus / 1_000);
        }
        return String.valueOf((int) bonus);
    }

    public static String convertDurationToString(Long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder duration = new StringBuilder();

        if (hours > 0) {
            duration.append(hours).append(" hrs ");
        }
        if (minutes > 0) {
            duration.append(minutes).append(" min");
        }
        return duration.toString().trim();
    }
}
