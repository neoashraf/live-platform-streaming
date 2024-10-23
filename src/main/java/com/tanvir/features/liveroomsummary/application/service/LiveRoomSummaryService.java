package com.tanvir.features.liveroomsummary.application.service;

import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.giftsummary.application.port.out.GiftSummaryPersistencePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroomsummary.adapter.out.persistence.entity.LiveRoomSummaryEntity;
import com.tanvir.features.liveroomsummary.application.port.in.LiveRoomSummaryUseCase;
import com.tanvir.features.liveroomsummary.application.port.out.LiveRoomSummaryPersistencePort;
import com.tanvir.features.liveroomsummary.domain.LiveRoomSummary;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.security.web.reactive.result.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class LiveRoomSummaryService implements LiveRoomSummaryUseCase {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Autowired
    private LiveRoomSummaryPersistencePort port;

    private final ModelMapper modelMapper;
    private CurrentSecurityContextArgumentResolver reactiveCurrentSecurityContextArgumentResolver;

    public LiveRoomSummaryService(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
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

        // Determine the day end time for logic
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


        ZonedDateTime finalCurrentUTC = currentUTC;
        return latestEntryMono.flatMap(summary -> {
            log.info("Found existing summary for user : {}", userId);
            return updateSummary(summary, durationInSeconds, liveRoom);
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
            newSummary.setDayTime(durationInSeconds >= 3600 ? "Yes" : "No");
            newSummary.setMonth(finalCurrentUTC.getMonthValue());
            newSummary.setYear(finalCurrentUTC.getYear());

            // Set session details
            LiveRoomSummaryEntity.SessionDetail newSessionDetail = new LiveRoomSummaryEntity.SessionDetail();
            newSessionDetail.setLiveRoomId(liveRoom.getId());
            newSessionDetail.setDuration(durationInSeconds);
            newSummary.setSessionDetails(Collections.singletonList(newSessionDetail));

            newSummary.setCreatedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());

            // Save the new summary back to the database
            return reactiveMongoTemplate.save(newSummary)
                    .doOnNext(liveRoomSummaryEntity -> log.info("Created summary for user : {}", liveRoomSummaryEntity))
                    .map(liveRoomSummaryEntity -> modelMapper.map(liveRoomSummaryEntity, LiveRoomSummary.class)); // Return the liveRoomSummaryEntity wrapped in Mono
        }));
    }

    private Mono<LiveRoomSummary> updateSummary(LiveRoomSummaryEntity summary, long durationInSeconds, LiveRoom liveRoom) {
        summary.setTotalDuration(summary.getTotalDuration() + durationInSeconds);
        summary.setTotalDurationString(CommonBusiness.formatTimeToString(summary.getTotalDuration()));
        summary.setTotalSessions(summary.getTotalSessions() + 1);

        // Add new session details
        List<LiveRoomSummaryEntity.SessionDetail> sessionDetails = new ArrayList<>(summary.getSessionDetails());
        LiveRoomSummaryEntity.SessionDetail newSessionDetail = new LiveRoomSummaryEntity.SessionDetail();
        newSessionDetail.setLiveRoomId(liveRoom.getId());
        newSessionDetail.setDuration(durationInSeconds);
        sessionDetails.add(newSessionDetail);
        summary.setSessionDetails(sessionDetails);

        // Check if dayTime should be set to "Yes"
        if (summary.getTotalDuration() >= 3600) {
            summary.setDayTime("Yes");
        }

        summary.setUpdatedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());

        // Save the updated summary back to the database
        return reactiveMongoTemplate.save(summary)
                .doOnNext(liveRoomSummaryEntity -> log.info("Updated summary for user : {}", liveRoomSummaryEntity))
                .map(liveRoomSummaryEntity -> modelMapper.map(liveRoomSummaryEntity, LiveRoomSummary.class)); // Return the liveRoom wrapped in Mono
    }


}
