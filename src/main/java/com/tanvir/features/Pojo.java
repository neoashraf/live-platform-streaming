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
        /*ModelMapper modelMapper = new ModelMapper();

        LiveRoom liveRoom = LiveRoom
                .builder()
                .duration(604L)
                .build();

        LiveStreamInfo liveStreamInfo = modelMapper.map(liveRoom, LiveStreamInfo.class);
        System.out.println(liveStreamInfo);

        DecimalFormat decimalFormat = new DecimalFormat("00");


        long hours = liveRoom.getDuration() / 3600;
        long minutes = (liveRoom.getDuration() % 3600) / 60;
        long seconds = (liveRoom.getDuration() - (hours * 3600) - (minutes * 60));

        liveStreamInfo.setDurationString(minutes + " minute(s) " + seconds + " seconds");
        liveStreamInfo.setDurationString(decimalFormat.format(hours) + ":" + decimalFormat.format(minutes) + ":" + decimalFormat.format(seconds));

        System.out.println(liveStreamInfo);*/

        /*User fanUser = User
                .builder()
                .beansCount(100000L)
                .build();
        User host = User
                .builder()
//                .beansCount(3000000L)
//                .gemsCount(0L)
                .build();

        LiveRoom liveRoom = LiveRoom
                .builder()
//                .beansCount(0L)
//                .starCount(0)
                .build();
        SendGiftRequestDto requestDto = SendGiftRequestDto
                .builder()
                .giftBeans(25200)
                .build();
        MetaProperty metaProperty = MetaProperty
                .builder()
                .starIndex(10000)
                .gemsConversionRate(50.0)
                .build();

        System.out.println("Start");


        fanUser.setBeansCount(fanUser.getBeansCount() - requestDto.getGiftBeans());
        liveRoom.setBeansCount(liveRoom.getBeansCount() == null
                ? requestDto.getGiftBeans()
                : liveRoom.getBeansCount() + requestDto.getGiftBeans());
        liveRoom.setStarCount((int) (liveRoom.getBeansCount() / metaProperty.getStarIndex()));
        int remainingBeans = (int) (liveRoom.getBeansCount() % (liveRoom.getStarCount() * metaProperty.getStarIndex()));
        liveRoom.setLevelCompletionPercentage((remainingBeans * 100) / metaProperty.getStarIndex());


        host.setBeansCount(host.getBeansCount() == null
                ? requestDto.getGiftBeans()
                : host.getBeansCount() + requestDto.getGiftBeans());
        host.setGemsCount(host.getGemsCount() == null
                ? (long)(requestDto.getGiftBeans() * (metaProperty.getGemsConversionRate() / 100))
                : host.getGemsCount() + (long)(requestDto.getGiftBeans() * (metaProperty.getGemsConversionRate() / 100)));

        System.out.println("fanUser beans : " + fanUser.getBeansCount());
        System.out.println("LiveRoom beans : " + liveRoom.getBeansCount());
        System.out.println("LiveRoom stars : " + liveRoom.getStarCount());
        System.out.println("remainingBeans : " + remainingBeans);
        System.out.println("level complete % : " + liveRoom.getLevelCompletionPercentage());
        System.out.println("host beans : " + host.getBeansCount());
        System.out.println("host gems : " + host.getGemsCount());*/
    }
}
