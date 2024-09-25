package com.tanvir.features;

import com.tanvir.features.liveroom.application.port.in.dto.request.SendGiftRequestDto;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroom.domain.valueobject.LiveStreamInfo;
import com.tanvir.features.metaproperty.domain.MetaProperty;
import com.tanvir.features.user.domain.User;
import org.modelmapper.ModelMapper;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;

public class Pojo {
    public static void main(String[] args) {
        System.out.println(formatToK(1140));  // 30K
        System.out.println(formatToK(32000));  // 32K
        System.out.println(formatToK(32500));  // 32.5K
        System.out.println(formatToK(100000));
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
