package com.tanvir.features.liveroomsummary.application.service;

import com.tanvir.features.giftsummary.application.port.out.GiftSummaryPersistencePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroomsummary.adapter.out.persistence.entity.LiveRoomSummaryEntity;
import com.tanvir.features.liveroomsummary.application.port.in.LiveRoomSummaryUseCase;
import com.tanvir.features.liveroomsummary.application.port.out.LiveRoomSummaryPersistencePort;
import com.tanvir.features.liveroomsummary.domain.LiveRoomSummary;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    public LiveRoomSummaryService(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }


    @Override
    public Mono<LiveRoomSummary> processLiveRoomSummary(LiveRoom liveRoom) {
        String userId = liveRoom.getUserId();
        long durationInSeconds = liveRoom.getDurationInSeconds();

        // Determine the current date based on UTC 1 AM rule
        LocalDateTime currentTime =LocalDateTime.now().atOffset(ZoneOffset.UTC).toLocalDateTime();
        LocalDate currentDate = LocalDateTime.now().atOffset(ZoneOffset.UTC).toLocalDate();
        LocalDateTime liveRoomCreationTime = liveRoom.getCreatedOn().atOffset(ZoneOffset.UTC).toLocalDateTime();

        log.info("currentTime : {}", currentTime);
        log.info("currentDate : {}", currentDate);
        log.info("liveRoomCreationTime : {}", liveRoomCreationTime);
        if (liveRoomCreationTime.isBefore(currentDate.atTime(7, 0))) {
            log.info("Before 1 AM UTC, set to the previous day");
            currentDate = currentDate.minusDays(1); // Before 1 AM UTC, set to the previous day
        }

        // Query for existing LiveRoomSummaryEntity by userId and date
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId)
                .and("date").is(currentDate.toString()));

        // Find the existing summary and update or create a new one reactively
        LocalDate finalCurrentDate = currentDate;
        return reactiveMongoTemplate.findOne(query, LiveRoomSummaryEntity.class)
                .flatMap(summary -> {
                    log.info("Found existing summary for user : {}", userId);
                    // Update existing fields
                    summary.setTotalDuration(summary.getTotalDuration() + durationInSeconds);
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

                    summary.setUpdatedOn(LocalDateTime.now());

                    // Save the updated summary back to the database
                    return reactiveMongoTemplate.save(summary)
                            .doOnNext(liveRoomSummaryEntity -> log.info("Updated summary for user : {}", liveRoomSummaryEntity))
                            .map(liveRoomSummaryEntity -> modelMapper.map(liveRoomSummaryEntity, LiveRoomSummary.class)); // Return the liveRoom wrapped in Mono
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Create new LiveRoomSummaryEntity if it does not exist
                    log.info("Creating a new summary for user : {}", userId);
                    LiveRoomSummaryEntity newSummary = new LiveRoomSummaryEntity();
                    newSummary.setId(UUID.randomUUID().toString());
                    newSummary.setUserId(userId);
                    newSummary.setDate(finalCurrentDate.toString());
                    newSummary.setTotalDuration(durationInSeconds);
                    newSummary.setTotalSessions(1);
                    newSummary.setDayTime("No");

                    // Set session details
                    LiveRoomSummaryEntity.SessionDetail newSessionDetail = new LiveRoomSummaryEntity.SessionDetail();
                    newSessionDetail.setLiveRoomId(liveRoom.getId());
                    newSessionDetail.setDuration(durationInSeconds);
                    newSummary.setSessionDetails(Collections.singletonList(newSessionDetail));

                    // Set dayTime if duration >= 3600
                    if (durationInSeconds >= 3600) {
                        newSummary.setDayTime("Yes");
                    }

                    newSummary.setCreatedOn(LocalDateTime.now());
                    newSummary.setUpdatedOn(LocalDateTime.now());

                    // Save the new summary back to the database
                    return reactiveMongoTemplate.save(newSummary)
                            .doOnNext(liveRoomSummaryEntity -> log.info("Created summary for user : {}", liveRoomSummaryEntity))
                            .map(liveRoomSummaryEntity -> modelMapper.map(liveRoomSummaryEntity, LiveRoomSummary.class)); // Return the liveRoomSummaryEntity wrapped in Mono
                }));
    }

}
