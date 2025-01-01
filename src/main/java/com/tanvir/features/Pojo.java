package com.tanvir.features;

import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.level.domain.Level;
import com.tanvir.features.liveroom.application.port.in.dto.request.SendGiftRequestDto;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.LiveStreamInfo;
import com.tanvir.features.metaproperty.domain.MetaProperty;
import com.tanvir.features.user.domain.User;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class Pojo {
    public static void main(String[] args) {
        /*System.out.println(formatToK(1140));  // 30K
        System.out.println(formatToK(32000));  // 32K
        System.out.println(formatToK(32500));  // 32.5K
        System.out.println(formatToK(100000));

        List<Integer> integerList = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        System.out.println("before : " + integerList);
        if (integerList.size() >= 10) {
            integerList = integerList.subList(integerList.size() - 9, integerList.size());
        }
        integerList.add(11);
        System.out.println("after : " + integerList);

        List<String> fruits = new ArrayList<>(List.of("Apple"));
        System.out.println("fruits : " + fruits);
        fruits.remove("Banana");
        System.out.println("fruits after remove : " + fruits);*/

//        double value = 5.2E7;
//        double value = 5.2123445;
//        BigDecimal result = convertToPlainBigDecimal(value);
//        System.out.println("Converted value: " + result);  // Output: 52000000

        // Get the current date and time in UTC
        ZonedDateTime birthday = ZonedDateTime.now(ZoneOffset.UTC);
        Instant instant = ZonedDateTime.now(ZoneOffset.UTC).toInstant();

        // Get the day of the month (UTC)
        int date1 = birthday.getDayOfMonth();

        // Print the full birthday and the day of the month
        System.out.println("Birthday Zoned: " + birthday);
        System.out.println("Birthday Instant: " + instant);
        System.out.println("Day of the month (UTC): " + date1);

        System.out.println("Zoned string: " + birthday.toString());
        System.out.println("Instant string: " + instant.toString());


    }

    public static BigDecimal convertToPlainBigDecimal(double value) {
        // Convert the double to a string, then to BigDecimal to avoid precision issues
        BigDecimal bigDecimalValue = new BigDecimal(Double.toString(value));

        // Return the plain string format of the BigDecimal
        return new BigDecimal(bigDecimalValue.toPlainString());
    }

    public static String formatToK(double value) {
        if (value >= 1000) {
            double kValue = value / 1000;
            if (Math.abs(kValue - Math.round(kValue)) < 0.1) {
                return String.format("%dK", Math.round(kValue));
            } else {
                return String.format("%.1fK", kValue);
            }
        }
        return String.valueOf((long) value);
    }
}
