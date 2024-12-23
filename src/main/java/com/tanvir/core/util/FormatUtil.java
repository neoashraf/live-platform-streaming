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
        long hours = totalSeconds / 3600; // Total hours
        long minutes = (totalSeconds % 3600) / 60; // Remaining minutes

        StringBuilder duration = new StringBuilder();

        if (hours > 0) {
            duration.append(hours).append(" hr").append(hours > 1 ? "s " : " ");
        }
        if (minutes > 0) {
            duration.append(minutes).append(" min").append(minutes > 1 ? "s " : " ");
        }

        return duration.toString().trim();
    }

    public static String convertToShortName(Double value) {
        if (value >= 1e9) {
            // For billions
            return formatValue(value / 1e9, "B");
        } else if (value >= 1e6) {
            // For millions
            return formatValue(value / 1e6, "M");
        } else if (value >= 1e3) {
            // For thousands
            return formatValue(value / 1e3, "K");
        } else {
            // For values below 1,000, avoid decimal if whole number
            return formatValue(value, "");
        }
    }

    private static String formatValue(double value, String suffix) {
        // If the value is a whole number, format without decimals
        if (value == Math.floor(value)) {
            return String.format("%.0f%s", value, suffix); // No decimal if it's a whole number
        } else {
            // Format with up to 2 decimal places if value has fractional part
            return String.format("%.2f%s", value, suffix);
        }
    }

}
