package com.tanvir.features.commonbusiness;

import com.tanvir.core.util.enums.AnnouncementEnum;
import com.tanvir.core.util.enums.ResourceTypeEnum;
import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.level.domain.Level;
import com.tanvir.features.level.domain.valueobjects.ResourceFormat;
import com.tanvir.features.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.bson.internal.BsonUtil;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
@Component
@Slf4j
public class CommonBusiness {

    private final LevelUseCase levelUseCase;

    public CommonBusiness(LevelUseCase levelUseCase) {
        this.levelUseCase = levelUseCase;
    }

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
            return formatValue(value / 1e9, "B");
        } else if (value >= 1e6) {
            return formatValue(value / 1e6, "M");
        } else if (value >= 1e3) {
            return formatValue(value / 1e3, "K");
        } else {
            return formatValue(value, "");
        }
    }

    private static String formatValue(Double value, String suffix) {
        if (value % 1 == 0) {
            return String.format("%.0f%s", value, suffix); // No decimals for whole numbers
        } else {
            return String.format("%.2f%s", Math.floor(value * 100) / 100, suffix); // Truncate to 2 decimal places
        }
    }

    public static String getAnnouncementMessage(String announcementType) {
        Map<String, String> announcementMessage = Map.of(
                AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_CASUAL.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_JOIN_CASUAL.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_RIDE.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_JOIN_RIDE_ENTRY.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_JOIN_ENTRY_CARD.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_JOIN_ENTRY_CARD_ENTRY.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_KICK.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_KICK.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_COMMENT.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_COMMENT.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_GIFT.getValue(),
                AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT_UPDATE.getValue(), AnnouncementEnum.ANNOUNCEMENT_MESSAGE_GIFT_UPDATE.getValue()
        );

        return announcementMessage.get(announcementType);
    }

    public static String formatTimeToString(long durationInSeconds) {

        int hours = (int) durationInSeconds / 3600;
        int minutes = (int) (durationInSeconds % 3600) / 60;
        int seconds = (int) durationInSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static BigDecimal convertToPlainBigDecimal(double value) {
        // Convert the double to a string, then to BigDecimal to avoid precision issues
        BigDecimal bigDecimalValue = new BigDecimal(Double.toString(value));

        // Return the plain string format of the BigDecimal
        return new BigDecimal(bigDecimalValue.toPlainString());
    }

    public static Integer calculateLevel(Double beans, List<Level> levelList) {
        List<Level> sortedLevels = new ArrayList<>(levelList);
        sortedLevels.sort(Comparator.comparing(Level::getNextLevelExpTargetValue));

        int currentLevel = 0;

        for (Level level : levelList) {
            if (beans < level.getNextLevelExpTargetValue()) {
                currentLevel = level.getLevel();
                break;
            }
        }

        if (beans >= sortedLevels.get(sortedLevels.size() - 1).getNextLevelExpTargetValue()) {
            return sortedLevels.get(sortedLevels.size() - 1).getLevel();
        }

        return currentLevel;
    }


    public static ResourceFormat getResourceFormatByResourceType(List<ResourceFormat> resourceFormats, String resourceType) {
        for (ResourceFormat resourceFormat : resourceFormats) {
            if (resourceFormat.getResourceType().equals(resourceType)) {
                return resourceFormat;
            }
        }
        return ResourceFormat.builder().build();
    }

    public Mono<User> setUserLevelUrl(User user) {
        return levelUseCase.getLevelDomainByLevel(user.getUserLevel())
                .map(level -> {
                    ResourceFormat levelResource = CommonBusiness.getResourceFormatByResourceType(level.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                    user.setLevelBadgeUrl(levelResource.getResourceUrl());
                    return user;
                })
                .doOnError(throwable -> log.error("Error while setting user level url: {}", throwable.getMessage()));
    }

    public static boolean areDatesEqual(String stringDate, Instant instantDate) {
        if (instantDate == null) {
            return false;
        }

        LocalDate parsedStringDate = LocalDate.parse(stringDate);
        LocalDate instantLocalDate = instantDate.atZone(ZoneOffset.UTC).toLocalDate();

        return parsedStringDate.equals(instantLocalDate);
    }

    public static String formatInstantToDate(Instant instant) {

        // Convert Instant to LocalDate
        LocalDate localDate = instant.atZone(ZoneOffset.UTC).toLocalDate();

        // Format the LocalDate to the desired format (yyyy-MM-dd)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return localDate.format(formatter);
    }



}
