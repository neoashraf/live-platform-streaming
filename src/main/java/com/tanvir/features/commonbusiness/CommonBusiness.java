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

}
