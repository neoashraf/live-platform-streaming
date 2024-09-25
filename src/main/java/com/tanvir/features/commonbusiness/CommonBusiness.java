package com.tanvir.features.commonbusiness;

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

    private static String convertToShortName(Double value) {
        if (value >= 1e9) {
            return String.format("%.3fB", value / 1e9);
        }
        else if (value >= 1e6) {
            return String.format("%.3fM", value / 1e6);
        }
        else if (value >= 1e3) {
            return String.format("%.3fK", value / 1e3);
        }
        else {
            return String.format("%.3f", value);
        }
    }

}
