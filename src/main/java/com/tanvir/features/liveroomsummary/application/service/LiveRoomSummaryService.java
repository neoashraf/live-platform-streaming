package com.tanvir.features.liveroomsummary.application.service;

import com.tanvir.core.util.Constants;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.giftsummary.application.port.out.GiftSummaryPersistencePort;
import com.tanvir.features.host.application.port.out.HostPersistencePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroomsummary.adapter.out.persistence.entity.LiveRoomSummaryEntity;
import com.tanvir.features.liveroomsummary.application.port.in.LiveRoomSummaryUseCase;
import com.tanvir.features.liveroomsummary.application.port.out.LiveRoomSummaryPersistencePort;
import com.tanvir.features.liveroomsummary.domain.LiveRoomSummary;
import com.tanvir.features.liveroomsummary.domain.dto.HostEarning;
import com.tanvir.features.liveroomsummary.domain.dto.HostEarningRequestDto;
import com.tanvir.features.liveroomsummary.domain.dto.HostEarningResponseDto;
import com.tanvir.features.liveroomsummary.domain.dto.HostEarningSummaryDto;
import com.tanvir.features.user.application.port.in.UserUseCase;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.security.web.reactive.result.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LiveRoomSummaryService implements LiveRoomSummaryUseCase {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private LiveRoomSummaryPersistencePort port;

    private final ModelMapper modelMapper;
    private final UserUseCase userUseCase;
    private final HostPersistencePort hostPersistencePort;

    public LiveRoomSummaryService(ModelMapper modelMapper, UserUseCase userUseCase, HostPersistencePort hostPersistencePort) {
        this.modelMapper = modelMapper;
        this.userUseCase = userUseCase;
        this.hostPersistencePort = hostPersistencePort;
    }


    @Override
    public Mono<LiveRoomSummary> processLiveRoomSummary(LiveRoom liveRoom) {
        String userId = liveRoom.getUserId();
//        long durationInSeconds = 4000; // Example duration, you may uncomment the next line if needed
         long durationInSeconds = liveRoom.getDurationInSeconds();

        ZonedDateTime currentUTC = ZonedDateTime.now(ZoneOffset.UTC);
        /*ZonedDateTime currentUTC = ZonedDateTime.of(
                2025, 1, 2, // Year, Month, Day
                1, 0, 0, 0, // Hour (1 AM), Minute, Second, Nanosecond
                ZoneOffset.UTC // Time zone offset (UTC)
        );*/
        log.info("Current UTC time: {}", currentUTC);

        // Determine the day end time for logic yyyy-mm-ddT01:00:00Z
        ZonedDateTime dayEndTime = currentUTC.withHour(1).withMinute(0).withSecond(0).withNano(0);

        // Adjust current date based on the time
        if (currentUTC.isBefore(dayEndTime)) {
            log.info("Current UTC time is before 1 AM");
            currentUTC = currentUTC.minusDays(1); // Use the previous day
        }

        // Set the date to check against
        String dateToCheck = currentUTC.toLocalDate().toString();

        // Query for existing LiveRoomSummaryEntity by userId and date
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId)
                .and("date").is(dateToCheck));

        // Get the latest entry (sorted by createdOn in descending order)
        query.with(Sort.by(Sort.Direction.DESC, "createdOn")).limit(1);
        Mono<LiveRoomSummaryEntity> latestEntryMono = reactiveMongoTemplate.findOne(query, LiveRoomSummaryEntity.class);

        boolean isEligibleForBonus = durationInSeconds >= Constants.MINIMUM_DURATION_FOR_GEMS_REWARD
                && liveRoom.getType().equalsIgnoreCase("video");


        ZonedDateTime finalCurrentUTC = currentUTC;
        return latestEntryMono.flatMap(summary -> {
            log.info("Found existing summary for user : {}", userId);
            return updateSummary(summary, durationInSeconds, liveRoom, finalCurrentUTC, isEligibleForBonus);
        }).switchIfEmpty(Mono.defer(() -> {
            // No existing summary found; check the conditions to create a new one
            log.info("Creating a new summary for user : {}", userId);
            LiveRoomSummaryEntity newSummary = new LiveRoomSummaryEntity();
            newSummary.setId(UUID.randomUUID().toString());
            newSummary.setUserId(userId);
            newSummary.setDate(finalCurrentUTC.toLocalDate().toString());
            newSummary.setTotalDuration(durationInSeconds);
            newSummary.setTotalDurationString(CommonBusiness.formatTimeToString(durationInSeconds));
            newSummary.setTotalSessions(1);
            newSummary.setHostDailyGems(liveRoom.getHostDailyGems());
            newSummary.setDayTime(durationInSeconds >= 3600 ? "Yes" : "No");
            newSummary.setMonth(finalCurrentUTC.getMonthValue());
            newSummary.setYear(finalCurrentUTC.getYear());

            // Set session details
            LiveRoomSummaryEntity.SessionDetail newSessionDetail = new LiveRoomSummaryEntity.SessionDetail();
            newSessionDetail.setLiveRoomId(liveRoom.getId());
            newSessionDetail.setDuration(durationInSeconds);
            newSessionDetail.setHostDailyGems(liveRoom.getHostDailyGems());

            if (isEligibleForBonus) {
                newSessionDetail.setBonus(Constants.DAILY_GEMS_REWARD_AMOUNT);
                newSessionDetail.setHostDailyGems(liveRoom.getHostDailyGems() + Constants.DAILY_GEMS_REWARD_AMOUNT);
                newSummary.setLastGemsAwardedDate(finalCurrentUTC.toLocalDate().toString());

                // Update gems in user table
                userUseCase.addMoreGems(liveRoom.getHostMaxId(), Constants.DAILY_GEMS_REWARD_AMOUNT)
                        .subscribe(
                                updatedUser -> log.info("User gems updated successfully: {}", updatedUser),
                                error -> log.error("Failed to update user gems", error)
                        );

                // Update gems in table
                hostPersistencePort.addMoreGems(liveRoom.getUserId(), Constants.DAILY_GEMS_REWARD_AMOUNT)
                        .doOnSuccess(updatedHost -> log.info("Host gems updated successfully: {}", updatedHost))
                        .doOnError(error -> log.error("Failed to update host gems for UserId {}: {}", liveRoom.getUserId(), error.getMessage()))
                        .subscribe();
            }


            newSummary.setSessionDetails(Collections.singletonList(newSessionDetail));

            if (durationInSeconds >= Constants.MINIMUM_DURATION_FOR_DAY_INCREMENT && liveRoom.getType().equalsIgnoreCase("video")) {
                newSummary.setTotalLiveDays(newSummary.getTotalLiveDays() + 1);
                newSummary.setLastDayCountedDate(finalCurrentUTC.toLocalDate().toString());
            }
            newSummary.setCreatedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
            newSummary.setHostDailyGems(newSessionDetail.getHostDailyGems());

            // Save the new summary back to the database
            return reactiveMongoTemplate.save(newSummary)
                    .doOnNext(liveRoomSummaryEntity -> log.info("Created summary for user : {}", liveRoomSummaryEntity))
                    .map(liveRoomSummaryEntity -> modelMapper.map(liveRoomSummaryEntity, LiveRoomSummary.class)); // Return the liveRoomSummaryEntity wrapped in Mono
        }));
    }

    @Override
    public Mono<HostEarningResponseDto> getHostEarnings(HostEarningRequestDto requestDto) {
       return userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
               .flatMap(user -> this.findTotalHostDailyGemsWithDuration(user.getId(), requestDto.getMonth(), requestDto.getYear()))
                .map(hostEarningSummaryDto -> {
                    log.info("Host earning summary: {}", hostEarningSummaryDto);
                    return HostEarning.builder()
                            .totalGems(hostEarningSummaryDto.getTotalHostDailyGems())
                            .totalGemsValue(CommonBusiness.convertToShortName((double) hostEarningSummaryDto.getTotalHostDailyGems()))
                            .totalDuration(hostEarningSummaryDto.getTotalDuration())
                            .totalDurationString(CommonBusiness.formatTimeToString(hostEarningSummaryDto.getTotalDuration()))
                            .totalDayTimeCount(hostEarningSummaryDto.getDayTimeCount())
                            .build();
                })
               .map(hostEarning -> HostEarningResponseDto.builder()
                       .message("Host earning details fetched successfully")
                       .data(hostEarning)
                       .count(1)
                       .build());
    }

    public Mono<HostEarningSummaryDto> findTotalHostDailyGemsWithDuration(String userId, int month, int year) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("userId").is(userId)
                                .and("month").is(month)
                                .and("year").is(year)
                ),
                Aggregation.group()
                        .sum("hostDailyGems").as("totalHostDailyGems")
                        .sum("totalDuration").as("totalDuration")
                        .sum(ConditionalOperators.when(Criteria.where("dayTime").is("Yes")).then(1).otherwise(0)).as("dayTimeCount") // Counts dayTime = "Yes"
        );

        return reactiveMongoTemplate.aggregate(aggregation, "liveroom_summary", HostEarningSummaryDto.class)
                .next()
                .defaultIfEmpty(new HostEarningSummaryDto(0L, 0L, 0)); // Default DTO if no matching records
    }


    private Mono<LiveRoomSummary> updateSummary(LiveRoomSummaryEntity summary, long durationInSeconds, LiveRoom liveRoom, ZonedDateTime finalCurrentUTC, boolean isEligibleForBonus) {
        summary.setTotalDuration(summary.getTotalDuration() + durationInSeconds);
        summary.setTotalDurationString(CommonBusiness.formatTimeToString(summary.getTotalDuration()));
        summary.setHostDailyGems(liveRoom.getHostDailyGems());
        summary.setTotalSessions(summary.getTotalSessions() + 1);

        // Add new session details
        List<LiveRoomSummaryEntity.SessionDetail> sessionDetails = Optional.ofNullable(summary.getSessionDetails())
                .orElseGet(ArrayList::new);

        LiveRoomSummaryEntity.SessionDetail newSessionDetail = new LiveRoomSummaryEntity.SessionDetail();
        newSessionDetail.setLiveRoomId(liveRoom.getId());
        newSessionDetail.setDuration(durationInSeconds);
        newSessionDetail.setHostDailyGems(liveRoom.getHostDailyGems());
        sessionDetails.add(newSessionDetail);
        summary.setSessionDetails(sessionDetails);

        String currentDate = finalCurrentUTC.toLocalDate().toString();
        if (isEligibleForBonus && (summary.getLastGemsAwardedDate() == null || !summary.getLastGemsAwardedDate().equals(currentDate))
        ) {
            addBonusToSession(summary, liveRoom, currentDate);
        }


        // increment totalLiveDays++ only once per day if 60+ minutes
        if (durationInSeconds >= Constants.MINIMUM_DURATION_FOR_DAY_INCREMENT
                && (summary.getLastDayCountedDate() == null || !summary.getLastDayCountedDate().equals(currentDate))
                && liveRoom.getType().equalsIgnoreCase("video")
        ) {
            summary.setTotalLiveDays(summary.getTotalLiveDays() + 1);
            summary.setLastDayCountedDate(currentDate);
        }

        // Check if dayTime should be set to "Yes"
        if (summary.getTotalDuration() >= Constants.MINIMUM_DURATION_FOR_DAY_INCREMENT) {
            summary.setDayTime("Yes");
        }

        summary.setUpdatedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());

        summary.setHostDailyGems(summary.getSessionDetails().stream().mapToDouble(LiveRoomSummaryEntity.SessionDetail::getHostDailyGems).sum());
        // Save the updated summary back to the database
        return reactiveMongoTemplate.save(summary)
                .doOnNext(liveRoomSummaryEntity -> log.info("Updated summary for user : {}", liveRoomSummaryEntity))
                .map(liveRoomSummaryEntity -> modelMapper.map(liveRoomSummaryEntity, LiveRoomSummary.class)); // Return the liveRoom wrapped in Mono
    }

    private void addBonusToSession(LiveRoomSummaryEntity summary, LiveRoom liveRoom, String currentDate) {
        liveRoom.setHostDailyGems(liveRoom.getHostDailyGems() + Constants.DAILY_GEMS_REWARD_AMOUNT);
        summary.getSessionDetails().stream()
                .filter(session -> session.getLiveRoomId().equals(liveRoom.getId()))
                .findFirst()
                .ifPresent(session -> session.setBonus(session.getBonus() + Constants.DAILY_GEMS_REWARD_AMOUNT));

        // Update gems in user table
        userUseCase.addMoreGems(liveRoom.getHostMaxId(), Constants.DAILY_GEMS_REWARD_AMOUNT)
                .doOnSuccess(updatedUser -> log.info("User gems updated successfully: {}", updatedUser))
                .doOnError(error -> log.error("Failed to update user gems for HostMaxId {}: {}", liveRoom.getHostMaxId(), error.getMessage()))
                .subscribe();

        // Update gems in host table
        hostPersistencePort.addMoreGems(liveRoom.getUserId(), Constants.DAILY_GEMS_REWARD_AMOUNT)
                .doOnSuccess(updatedHost -> log.info("Host gems updated successfully: {}", updatedHost))
                .doOnError(error -> log.error("Failed to update host gems for UserId {}: {}", liveRoom.getUserId(), error.getMessage()))
                .subscribe();

        summary.setLastGemsAwardedDate(currentDate);
    }

    public Mono<LiveRoomSummaryEntity> findLiveRoomSummary(String userId, int month, int year) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)
                        .and("month").is(month)
                        .and("year").is(year)),
                Aggregation.project()
                        .and("_id").as("id")
                        .and("month").as("month")
                        .and("year").as("year")
                        .and(ConditionalOperators.ifNull("hostDailyGems").then(0)).as("hostDailyGems")
                        .and(ConditionalOperators.ifNull("totalDuration").then(0)).as("totalDuration")
                        .and(ConditionalOperators.ifNull("totalBonus").then(0)).as("totalBonus")
                        .and(ConditionalOperators.ifNull("sessionDetails").then(Collections.emptyList())).as("sessionDetails")
        );

        return reactiveMongoTemplate.aggregate(agg, "liveroom_summary", LiveRoomSummaryEntity.class)
                .next();
    }







}
