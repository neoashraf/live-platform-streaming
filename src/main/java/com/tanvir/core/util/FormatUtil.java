package com.tanvir.core.util;

public class FormatUtil {

    public static String formatGems(double bonus) {
        if (bonus >= 1_000_000_000) {
            return String.format("%.1fB", bonus / 1_000_000_000);
        } else if (bonus >= 1_000_000) {
            return String.format("%.1fM", bonus / 1_000_000);
        } else if (bonus >= 1_000) {
            return String.format("%.1fK", bonus / 1_000);
        }
        return String.valueOf((int) bonus);
    }


    public static String convertDurationToString(Long totalSeconds) {
        long days = totalSeconds / 86400; // 1 day = 86400 seconds
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder duration = new StringBuilder();

        if (days > 0) {
            duration.append(days).append(" day").append(days > 1 ? "s " : " ");
        }
        if (hours > 0) {
            duration.append(hours).append(" hr").append(hours > 1 ? "s " : " ");
        }
        if (minutes > 0) {
            duration.append(minutes).append(" min").append(minutes > 1 ? "s " : " ");
        }
        if (seconds > 0) {
            duration.append(seconds).append(" sec").append(seconds > 1 ? "s" : "");
        }
        return duration.toString().trim();
    }

}
