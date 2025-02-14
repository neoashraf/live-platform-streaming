package com.tanvir.features.liveroomactivity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.*;

@Service
@Slf4j
public class LiveRoomActivityService {

    @Autowired
    private LiveRomActivityRepository liveroomActivityRepository; // Assume this is a reactive MongoDB repository

    public Mono<LiveRoomActivityEntity> updateDailyReceivedGems(String userId, Double gemsReceived) {
        Instant now = Instant.now();
//        Instant now = ZonedDateTime.of(2024, 10, 7, 23, 59,59,0, ZoneOffset.UTC).toInstant(); // Test
//        Instant now = ZonedDateTime.of(2024, 11, 10, 1, 0,0,0, ZoneOffset.UTC).toInstant(); // Test

        return liveroomActivityRepository.findByUserId(userId)
                .doOnRequest(l -> log.info("Request received to update daily received gems for user : {}", userId))
//                .doOnSuccess(activity -> log.info("Got activity for user : {}", activity))
                .flatMap(activity -> {
                    Instant activityEndTime = activity.getEndTime(); // Get the stored endTime in UTC
                    log.info("now : {}, activityEndTime : {}", now, activityEndTime);

                    // Check if `now` is before the `activityEndTime` (meaning it's the same day)
                    if (now.isBefore(activityEndTime)) {
                        log.info("Updating dailyReceivedGems for the same day for user : {}", userId);
                        activity.setDailyReceivedGems(activity.getDailyReceivedGems() + gemsReceived);
                        activity.setUpdatedOn(now);
                    }
                    else {
                        // If `now` is after `endTime`, reset the gems
                        log.info("Resetting dailyReceivedGems for user : {}", userId);
                        activity.setDailyReceivedGems(gemsReceived); // Reset to the current gems received

                        // Update `endTime` to the next day's 1 AM UTC
                        ZonedDateTime nextEndTime = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).plusDays(1).withHour(1).withMinute(0).withSecond(0).withNano(0);
                        activity.setEndTime(nextEndTime.toInstant());
                        log.info("New endTime set for user: {} is {}", userId, activity.getEndTime());

                        activity.setResetOn(now);
                    }

                    return liveroomActivityRepository.save(activity);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // No entry for the user, create a new one
                    log.info("Creating a new activity for user : {}", userId);
                    LiveRoomActivityEntity newActivity = new LiveRoomActivityEntity();
                    newActivity.setUserId(userId);
                    newActivity.setDailyReceivedGems(gemsReceived);

                    // Calculate the next day's 1 AM UTC based on whether the current time is after 12 AM
                    ZonedDateTime currentUTC = ZonedDateTime.ofInstant(now, ZoneOffset.UTC); // Test
                    ZonedDateTime nextEndTime;

                    if (currentUTC.getHour() == 0) {
                        // If it's before 1 AM UTC, set the endTime to 1 AM the same day
                        log.info("Current time is before 1 AM UTC");
                        nextEndTime = currentUTC.withHour(1).withMinute(0).withSecond(0).withNano(0);
                    } else {
                        // If it's after 1 AM UTC & before 12 AM UTC, add an extra day
                        log.info("Current time is after 1 AM UTC");
                        nextEndTime = currentUTC.plusDays(1).withHour(1).withMinute(0).withSecond(0).withNano(0);
                    }

                    newActivity.setEndTime(nextEndTime.toInstant());
                    log.info("New endTime set for new user: {} is {}", userId, newActivity.getEndTime());

                    newActivity.setCreatedOn(now);
                    return liveroomActivityRepository.save(newActivity);
                }));
    }


    public Mono<Double> getDailyReceivedGems(String userId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        return liveroomActivityRepository.findByUserId(userId)
                .doOnRequest(l -> log.info("Request received to get daily received gems for user : {}", userId))
                .doOnSuccess(activity -> log.info("Got activity for user : {}", activity))
                .map(activity -> {
                    Instant activityEndTime = activity.getEndTime();
                    log.info("now : {}, activityEndTime : {}", now, activityEndTime);

                    if (Instant.now().isAfter(activityEndTime)) {
                        log.info("Resetting dailyReceivedGems for user : {}", userId);
                        // If it's after 1 AM UTC, return 0 (since it should reset)
                        return 0.0;
                    } else {
                        log.info("Returning dailyReceivedGems for user : {}", userId);
                        // If before 1 AM UTC, return the current dailyReceivedGems
                        return activity.getDailyReceivedGems();
                    }
                })
                .switchIfEmpty(Mono.just(0.0)); // If no record is found, return 0
    }
}
