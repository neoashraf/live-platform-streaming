package com.tanvir.features.liveroomactivity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class LiveRoomActivityService {

    @Autowired
    private LiveRomActivityRepository liveroomActivityRepository; // Assume this is a reactive MongoDB repository

    public Mono<LiveRoomActivityEntity> updateDailyReceivedGems(String userId, Double gemsReceived) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
//        LocalDateTime now = LocalDateTime.of(2024, 10, 5, 1,0, 1).atZone(ZoneOffset.UTC).toLocalDateTime(); // For testing

        return liveroomActivityRepository.findByUserId(userId)
                .doOnRequest(l -> log.info("Request received to update daily received gems for user : {}", userId))
                .doOnSuccess(activity -> log.info("Got activity for user : {}", activity))
                .flatMap(activity -> {
                    LocalDateTime activityEndTime = activity.getEndTime().atZone(ZoneOffset.UTC).toLocalDateTime(); // Get the stored endTime
                    log.info("now : {}, activityEndTime : {}", now, activityEndTime);

                    // Check if `now` is after the `activityEndTime` (meaning we should reset the gems)
                    if (now.isAfter(activityEndTime)) {
                        log.info("Resetting dailyReceivedGems for user : {}", userId);

                        // Reset `dailyReceivedGems` and update `endTime` to the next day's 1 AM UTC
                        activity.setDailyReceivedGems(gemsReceived); // Reset to the current gems received
                        activity.setEndTime(
                                now.toLocalDate().plusDays(1)
                                        .atTime(1, 0)
                                        .atOffset(ZoneOffset.UTC)
                                        .toLocalDateTime()
                        );
                        log.info("New endTime set for user: {} is {}", userId, activity.getEndTime());

                        activity.setUpdatedOn(now);
                        activity.setResetOn(now);
                    } else {
                        // If `now` is not after `endTime`, just update the existing gems
                        log.info("Updating dailyReceivedGems for the same day for user : {}", userId);
                        activity.setDailyReceivedGems(activity.getDailyReceivedGems() + gemsReceived);
                        activity.setUpdatedOn(now);
                    }

                    return liveroomActivityRepository.save(activity);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // No entry for the user, create a new one
                    log.info("Creating a new activity for user : {}", userId);
                    LiveRoomActivityEntity newActivity = new LiveRoomActivityEntity();
                    newActivity.setUserId(userId);
                    newActivity.setDailyReceivedGems(gemsReceived);
                    newActivity.setEndTime(
                            now.toLocalDate().plusDays(1)
                                    .atTime(1, 0)
                                    .atOffset(ZoneOffset.UTC)
                                    .toLocalDateTime()
                    );
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
                    LocalDateTime activityEndTime = activity.getEndTime();
                    log.info("now : {}, activityEndTime : {}", now, activityEndTime);

                    if (now.isAfter(activityEndTime)) {
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
