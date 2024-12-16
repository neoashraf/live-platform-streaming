package com.tanvir.features.liveroomsummary.application.service;

import com.tanvir.core.util.Constants;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.gifttransaction.adapter.out.persistence.repository.GiftTransactionRepository;
import com.tanvir.features.gifttransaction.adapter.out.persistence.repository.GiftTransactionRepositoryCustomImpl;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePort;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import com.tanvir.features.gifttransaction.domain.LiveRoomTotalBeans;
import com.tanvir.features.host.application.port.out.HostPersistencePort;
import com.tanvir.features.host.domain.Host;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.repository.LiveRoomRepository;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroomsummary.adapter.out.persistence.entity.LiveRoomSummaryEntity;
import com.tanvir.features.liveroomsummary.adapter.out.persistence.repository.LiveRoomSummaryRepository;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private GiftTransactionPersistencePort giftTransactionPersistencePort;
    @Autowired
    private LiveRoomRepository liveRoomRepository;
    @Autowired
    private LiveRoomSummaryRepository liveRoomSummaryRepository;
    @Autowired
    private GiftTransactionRepository giftTransactionRepository;
    @Autowired
    private GiftTransactionRepositoryCustomImpl giftTransactionRepositoryCustomImpl;

    public LiveRoomSummaryService(ModelMapper modelMapper, UserUseCase userUseCase, HostPersistencePort hostPersistencePort, GiftTransactionPersistencePort giftTransactionPersistencePort) {
        this.modelMapper = modelMapper;
        this.userUseCase = userUseCase;
        this.hostPersistencePort = hostPersistencePort;
        this.giftTransactionPersistencePort = giftTransactionPersistencePort;
    }


    @Override
    public Mono<LiveRoomSummary> processLiveRoomSummary(LiveRoom liveRoom) {
        log.info("Live Room initial daily host GEMS:: {}", liveRoom.getHostDailyGems());
        String userId = liveRoom.getUserId();
        Mono<Host> dbHostUser = hostPersistencePort.getHostByUserId(userId);

        long durationInSeconds = liveRoom.getDurationInSeconds();
        ZonedDateTime currentUTC = ZonedDateTime.now(ZoneOffset.UTC);

        // Adjust for day-end logic
        ZonedDateTime dayEndTime = currentUTC.withHour(1).withMinute(0).withSecond(0).withNano(0);
        if (currentUTC.isBefore(dayEndTime)) {
            currentUTC = currentUTC.minusDays(1);
        }
        String dateToCheck = currentUTC.toLocalDate().toString();

        // Build query for existing summary
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("date").is(dateToCheck));
        query.with(Sort.by(Sort.Direction.DESC, "createdOn")).limit(1);

        // Fetch latest summary and calculate gift amount
        Mono<LiveRoomSummaryEntity> latestEntryMono = reactiveMongoTemplate.findOne(query, LiveRoomSummaryEntity.class);
        Mono<Double> receivedGiftAmountMono = giftTransactionPersistencePort.getGiftTransactions(liveRoom.getId())
                .filter(transaction -> transaction.getBeans() != null)
                .map(GiftTransaction::getBeans)
                .reduce(0.0, Double::sum)
                .defaultIfEmpty(0.0);

        // Calculate bonus eligibility reactively
        Mono<Boolean> isEligibleForBonusMono = dbHostUser.map(host ->
                         "video".equalsIgnoreCase(liveRoom.getType())
                        && "video".equalsIgnoreCase(host.getHostType())
        ).defaultIfEmpty(false); // Handle case where dbHostUser is empty

        ZonedDateTime finalCurrentUTC = currentUTC;

        return isEligibleForBonusMono.flatMap(isEligibleForBonus -> latestEntryMono.flatMap(summary -> {
            log.info("Found existing summary for user: {}", userId);
            // Update existing summary with duration, gifts, and bonus
            return receivedGiftAmountMono.flatMap(amount -> updateSummary(summary, durationInSeconds, liveRoom, finalCurrentUTC, isEligibleForBonus, amount));
        }).switchIfEmpty(Mono.defer(() -> {
            log.info("No existing summary found. Creating a new one for user: {}", userId);

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

            if (liveRoom.getType().equalsIgnoreCase("video")){
                newSummary.setTotalVideoDuration(durationInSeconds);
                newSummary.setTotalVideoDurationString(CommonBusiness.formatTimeToString(durationInSeconds));
            }

            if (liveRoom.getType().equalsIgnoreCase("audio")){
                newSummary.setTotalAudioDuration(durationInSeconds);
                newSummary.setTotalAudioDurationString(CommonBusiness.formatTimeToString(durationInSeconds));
            }

            // Create session details
            LiveRoomSummaryEntity.SessionDetail newSessionDetail = new LiveRoomSummaryEntity.SessionDetail();
            newSessionDetail.setCreatedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
            newSessionDetail.setLiveRoomId(liveRoom.getId());
            newSessionDetail.setDuration(durationInSeconds);
            newSessionDetail.setHostDailyGems(liveRoom.getHostDailyGems());
            newSessionDetail.setSessionType(liveRoom.getType());

            return receivedGiftAmountMono.flatMap(giftAmount -> {
                log.info("Total gift received amount: {}", giftAmount);
                newSessionDetail.setGiftReceivedAmount(giftAmount);

                if (isEligibleForBonus && (durationInSeconds >= Constants.MINIMUM_DURATION_FOR_GEMS_REWARD)) {
                    newSessionDetail.setBonus(Constants.DAILY_GEMS_REWARD_AMOUNT);
                    newSummary.setTotalBonus(Constants.DAILY_GEMS_REWARD_AMOUNT);
                    newSummary.setLastGemsAwardedDate(finalCurrentUTC.toLocalDate().toString());
                }
                newSummary.setSessionDetails(Collections.singletonList(newSessionDetail));
                return Mono.just(giftAmount);
            }).flatMap(amount -> {
                if (durationInSeconds >= Constants.MINIMUM_DURATION_FOR_DAY_INCREMENT && liveRoom.getType().equalsIgnoreCase("video") && isEligibleForBonus) {
                    newSummary.setTotalLiveDays(1);
                    newSummary.setLastDayCountedDate(finalCurrentUTC.toLocalDate().toString());
                }
                newSummary.setCreatedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
                newSummary.setHostDailyGems(newSessionDetail.getHostDailyGems());


                return reactiveMongoTemplate.save(newSummary)
                        .doOnNext(savedSummary -> log.info("Created summary for user: {}", savedSummary))
                        .map(savedSummary -> modelMapper.map(savedSummary, LiveRoomSummary.class));
            });
        })));
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


    private Mono<LiveRoomSummary> updateSummary(LiveRoomSummaryEntity summary, long durationInSeconds, LiveRoom liveRoom,
                                                ZonedDateTime finalCurrentUTC, boolean isEligibleForBonus,
                                                Double receivedGiftAmount) {

        // Update summary details
        summary.setTotalDurationString(CommonBusiness.formatTimeToString(summary.getTotalDuration()));
        summary.setTotalSessions(summary.getTotalSessions() + 1);

        // Add new session details
        List<LiveRoomSummaryEntity.SessionDetail> sessionDetails = Optional.ofNullable(summary.getSessionDetails())
                .orElseGet(ArrayList::new);

        LiveRoomSummaryEntity.SessionDetail newSessionDetail = new LiveRoomSummaryEntity.SessionDetail();
        newSessionDetail.setCreatedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
        newSessionDetail.setLiveRoomId(liveRoom.getId());
        newSessionDetail.setDuration(durationInSeconds);
        newSessionDetail.setHostDailyGems(liveRoom.getHostDailyGems());
        newSessionDetail.setGiftReceivedAmount(receivedGiftAmount);
        newSessionDetail.setSessionType(liveRoom.getType());
        sessionDetails.add(newSessionDetail);

        summary.setSessionDetails(sessionDetails);

        String currentDate = finalCurrentUTC.toLocalDate().toString();

        // Add bonus if eligible
        Mono<Void> bonusMono = Mono.empty();
        if (isEligibleForBonus && (durationInSeconds >= Constants.MINIMUM_DURATION_FOR_GEMS_REWARD) && (summary.getLastGemsAwardedDate() == null || !summary.getLastGemsAwardedDate().equals(currentDate))) {
            bonusMono = addBonusToSession(summary, liveRoom, currentDate);
        }

        // Increment totalLiveDays if conditions are met
        if (durationInSeconds >= Constants.MINIMUM_DURATION_FOR_DAY_INCREMENT
                && (summary.getLastDayCountedDate() == null || !summary.getLastDayCountedDate().equals(currentDate))
                && "video".equalsIgnoreCase(liveRoom.getType()) && isEligibleForBonus) {
            summary.setTotalLiveDays(summary.getTotalLiveDays() + 1);
            summary.setLastDayCountedDate(currentDate);
        }

        // Check if dayTime should be set to "Yes"
        if (summary.getTotalDuration() >= Constants.MINIMUM_DURATION_FOR_DAY_INCREMENT) {
            summary.setDayTime("Yes");
        }

        long totalVideoDurations = summary.getSessionDetails().stream().filter(session -> session.getSessionType().equalsIgnoreCase("video")).mapToLong(LiveRoomSummaryEntity.SessionDetail::getDuration).sum();
        long totalAudioDurations = summary.getSessionDetails().stream().filter(session -> session.getSessionType().equalsIgnoreCase("audio")).mapToLong(LiveRoomSummaryEntity.SessionDetail::getDuration).sum();
        double totalGiftReceivedAmount = summary.getSessionDetails().stream().mapToDouble(LiveRoomSummaryEntity.SessionDetail::getGiftReceivedAmount).sum();

        summary.setTotalVideoDuration(totalVideoDurations);
        summary.setTotalVideoDurationString(CommonBusiness.formatTimeToString(totalVideoDurations));

        summary.setTotalAudioDuration(totalAudioDurations);
        summary.setTotalAudioDurationString(CommonBusiness.formatTimeToString(totalAudioDurations));

        summary.setTotalDuration(totalVideoDurations + totalAudioDurations);
        summary.setTotalDurationString( CommonBusiness.formatTimeToString(totalVideoDurations + totalAudioDurations));

        summary.setTotalGiftReceivedAmount(totalGiftReceivedAmount);

        summary.setUpdatedOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant());

        // Update hostDailyGems to the latest session value
        summary.setHostDailyGems(
                summary.getSessionDetails().stream()
                        .max(Comparator.comparing(LiveRoomSummaryEntity.SessionDetail::getCreatedOn, Comparator.nullsFirst(Comparator.naturalOrder())))
                        .map(LiveRoomSummaryEntity.SessionDetail::getHostDailyGems)
                        .orElse(0.0)
        );

        // Save the updated summary and execute bonus logic reactively
        return bonusMono.then(
                reactiveMongoTemplate.save(summary)
                        .doOnNext(savedSummary -> log.info("Updated summary for user: {}", savedSummary))
                        .map(savedSummary -> modelMapper.map(savedSummary, LiveRoomSummary.class))
        );
    }


    private Mono<Void> addBonusToSession(LiveRoomSummaryEntity summary, LiveRoom liveRoom, String currentDate) {
        liveRoom.setHostDailyGems(liveRoom.getHostDailyGems() + Constants.DAILY_GEMS_REWARD_AMOUNT);

        // Update session bonus
        summary.getSessionDetails().stream()
                .filter(session -> session.getLiveRoomId().equals(liveRoom.getId()))
                .findFirst()
                .ifPresent(session -> {
                    session.setBonus(session.getBonus() + Constants.DAILY_GEMS_REWARD_AMOUNT);
                });

        summary.setTotalBonus(summary.getTotalBonus() + Constants.DAILY_GEMS_REWARD_AMOUNT);
        summary.setLastGemsAwardedDate(currentDate);

        return Mono.empty();
    }

    public Mono<LiveRoomSummaryEntity> findLiveRoomSummaryTotals(String userId, int month, int year) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)
                        .and("month").is(month)
                        .and("year").is(year)),
                Aggregation.project()
                        .and(ConditionalOperators.ifNull("hostDailyGems").then(0)).as("hostDailyGems")
                        .and(ConditionalOperators.ifNull("totalDuration").then(0)).as("totalDuration")
                        .and(ConditionalOperators.ifNull("totalVideoDuration").then(0)).as("totalVideoDuration")
                        .and(ConditionalOperators.ifNull("totalAudioDuration").then(0)).as("totalAudioDuration")
                        .and(ConditionalOperators.ifNull("totalGiftReceivedAmount").then(0)).as("totalGiftReceivedAmount")
                        .and(ConditionalOperators.ifNull("totalBonus").then(0)).as("totalBonus")
                        .and(ConditionalOperators.ifNull("totalLiveDays").then(0)).as("totalLiveDays"),
                Aggregation.group() // Grouping by null to compute totals across all matched records
                        .sum("hostDailyGems").as("hostDailyGems")
                        .sum("totalDuration").as("totalDuration")
                        .sum("totalVideoDuration").as("totalVideoDuration")
                        .sum("totalAudioDuration").as("totalAudioDuration")
                        .sum("totalGiftReceivedAmount").as("totalGiftReceivedAmount")
                        .sum("totalBonus").as("totalBonus")
                        .sum("totalLiveDays").as("totalLiveDays")
        );

        return reactiveMongoTemplate.aggregate(agg, "liveroom_summary", LiveRoomSummaryEntity.class)
                .next(); // Use .next() to get the first (and only) result as Mono
    }


    public Flux<LiveRoomSummaryEntity> findLiveRoomSummaries(String userId, int month, int year) {
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
                        .and(ConditionalOperators.ifNull("totalVideoDuration").then(0)).as("totalVideoDuration")
                        .and(ConditionalOperators.ifNull("totalAudioDuration").then(0)).as("totalAudioDuration")
                        .and(ConditionalOperators.ifNull("totalGiftReceivedAmount").then(0)).as("totalGiftReceivedAmount")
                        .and(ConditionalOperators.ifNull("totalBonus").then(0)).as("totalBonus")
                        .and(ConditionalOperators.ifNull("totalLiveDays").then(0)).as("totalLiveDays")
                        .and(ConditionalOperators.ifNull("sessionDetails").then(Collections.emptyList())).as("sessionDetails")
        );

        return reactiveMongoTemplate.aggregate(agg, "liveroom_summary", LiveRoomSummaryEntity.class);
    }

    public Mono<String> updateLiveRoomSummary() {
        Instant start = Instant.parse("2024-12-01T01:00:00Z");
        Instant end = Instant.parse("2024-12-16T01:00:00Z");

        Flux<LiveRoomEntity> dbAllLiveRoom = liveRoomRepository.findByCreatedOnBetween(start, end);
        Flux<LiveRoomSummaryEntity> dbAllLiveRoomSummary = liveRoomSummaryRepository.findByCreatedOnBetween(start, end);
        Flux<LiveRoomTotalBeans> dbAllGiftBean = giftTransactionRepositoryCustomImpl.findTotalBeansGroupedByLiveRoomId(start, end);

        // Combine all the data
        Mono.zip(dbAllLiveRoom.collectList(), dbAllLiveRoomSummary.collectList(), dbAllGiftBean.collectList())
                .flatMap(data -> {
                    List<LiveRoomEntity> liveRooms = data.getT1();
                    List<LiveRoomSummaryEntity> liveRoomSummaries = data.getT2();
                    List<LiveRoomTotalBeans> totalBeans = data.getT3();

                    // Iterate through each LiveRoomEntity and update corresponding summary
                    List<Mono<Void>> updateMonos = liveRooms.stream().map(liveRoom -> {

                        Mono<Host> dbHostUser = hostPersistencePort.getHostByUserId(liveRoom.getUserId());

                        Mono<Boolean> isEligibleForBonusMono = dbHostUser.map(host ->
                                "video".equalsIgnoreCase(liveRoom.getType())
                                        && "video".equalsIgnoreCase(host.getHostType())
                        ).defaultIfEmpty(false);


                        // Find the existing summary or create a new one
                        LiveRoomSummaryEntity existingSummary = liveRoomSummaries.stream()
                                .filter(summary ->
                                        summary.getUserId().equals(liveRoom.getUserId()) &&
                                                CommonBusiness.formatInstantToDate(summary.getCreatedOn()).equals(CommonBusiness.formatInstantToDate(liveRoom.getCreatedOn()))
                                )
                                .findFirst()
                                .orElse(null);



                        if (existingSummary != null) {
                            return updateSummary(existingSummary, liveRoom, totalBeans, isEligibleForBonusMono);
                        }
                        return null;
                    }).collect(Collectors.toList());

                    // Execute all updates asynchronously
                    return Mono.when(updateMonos);
                })
                .subscribe(
                        null,
                        error -> log.error("Error updating LiveRoom summaries", error),
                        () -> log.info("Successfully updated all LiveRoom summaries")
                );
        return Mono.just("Successfully updated all LiveRoom summaries");
    }

    private Mono<Void> updateSummary(LiveRoomSummaryEntity summary, LiveRoomEntity liveRoom,
                                     List<LiveRoomTotalBeans> totalBeans, Mono<Boolean> isEligibleForBonusMono) {
        log.info("Starting updateSummary for LiveRoom ID: {}", liveRoom.getId());

        LiveRoomSummaryEntity.SessionDetail sessionDetail = summary.getSessionDetails().stream()
                .filter(session -> session.getLiveRoomId().equals(liveRoom.getId()))
                .findFirst()
                .orElse(new LiveRoomSummaryEntity.SessionDetail());

        // Update session detail fields
        sessionDetail.setLiveRoomId(liveRoom.getId());
        sessionDetail.setDuration(liveRoom.getDurationInSeconds());
        sessionDetail.setGiftReceivedAmount(getGiftAmount(liveRoom, totalBeans));
        sessionDetail.setSessionType(liveRoom.getType());
        sessionDetail.setCreatedOn(liveRoom.getEndedOn());
        sessionDetail.setHostDailyGems(liveRoom.getHostDailyGems());

        return isEligibleForBonusMono.flatMap(isEligible -> {
            if (isEligible) {
                log.info("LiveRoom ID: {} is eligible for bonus.", liveRoom.getId());
                if (liveRoom.getDurationInSeconds() >= Constants.MINIMUM_DURATION_FOR_GEMS_REWARD &&
                        (summary.getLastGemsAwardedDate() == null ||
                                !summary.getLastGemsAwardedDate().equalsIgnoreCase(CommonBusiness.formatInstantToDate(liveRoom.getCreatedOn())))) {
                    sessionDetail.setBonus(10000);
                    summary.setLastGemsAwardedDate(CommonBusiness.formatInstantToDate(liveRoom.getEndedOn()));
                }
            }

            if (liveRoom.getDurationInSeconds() >= Constants.MINIMUM_DURATION_FOR_DAY_INCREMENT &&
                    (summary.getLastDayCountedDate() == null ||
                            !summary.getLastDayCountedDate().equalsIgnoreCase(CommonBusiness.formatInstantToDate(liveRoom.getCreatedOn()))) &&
                    isEligible) {
                summary.setTotalLiveDays(summary.getTotalLiveDays() + 1);
                summary.setLastDayCountedDate(CommonBusiness.formatInstantToDate(liveRoom.getEndedOn()));
                summary.setDayTime("Yes");
            }

            // Update totals
            updateTotals(summary);

            // Use a copy of the session details list
            List<LiveRoomSummaryEntity.SessionDetail> updatedSessionDetails = new ArrayList<>(summary.getSessionDetails());
            updatedSessionDetails.removeIf(session -> session.getLiveRoomId().equals(liveRoom.getId()));
            updatedSessionDetails.add(sessionDetail);
            summary.setSessionDetails(updatedSessionDetails);

            return reactiveMongoTemplate.save(summary)
                    .doOnSuccess(savedSummary -> log.info("LiveRoomSummary successfully updated for LiveRoom ID: {}", liveRoom.getId()))
                    .doOnError(error -> log.error("Error saving LiveRoomSummary for LiveRoom ID: {}", liveRoom.getId(), error))
                    .then();
        });
    }


    private double getGiftAmount(LiveRoomEntity liveRoom, List<LiveRoomTotalBeans> totalBeans) {
        // Find the total gift amount based on liveRoomId from totalBeans
        return totalBeans.stream()
                .filter(bean -> bean.getLiveRoomId() != null && bean.getLiveRoomId().equals(liveRoom.getId()))
                .map(LiveRoomTotalBeans::getTotalBeans)
                .findFirst()
                .orElse(0.0);
    }

    private void updateTotals(LiveRoomSummaryEntity summary) {
        // Recalculate the total values like duration, gifts, etc.
        long totalDuration = summary.getSessionDetails().stream().mapToLong(LiveRoomSummaryEntity.SessionDetail::getDuration).sum();
        double totalGiftReceivedAmount = summary.getSessionDetails().stream().mapToDouble(LiveRoomSummaryEntity.SessionDetail::getGiftReceivedAmount).sum();
        long totalVideoDuration = summary.getSessionDetails().stream()
                .filter(session -> "video".equalsIgnoreCase(session.getSessionType())) // Check for null
                .mapToLong(LiveRoomSummaryEntity.SessionDetail::getDuration)
                .sum();
        long totalAudioDuration = summary.getSessionDetails().stream()
                .filter(session -> "audio".equalsIgnoreCase(session.getSessionType())) // Check for null
                .mapToLong(LiveRoomSummaryEntity.SessionDetail::getDuration)
                .sum();

        summary.setTotalDuration(totalDuration);
        summary.setTotalDurationString(CommonBusiness.formatTimeToString(totalDuration));
        summary.setTotalVideoDuration(totalVideoDuration);
        summary.setTotalVideoDurationString(CommonBusiness.formatTimeToString(totalVideoDuration));
        summary.setTotalAudioDuration(totalAudioDuration);
        summary.setTotalAudioDurationString(CommonBusiness.formatTimeToString(totalAudioDuration));

        summary.setTotalGiftReceivedAmount(totalGiftReceivedAmount);

        summary.setHostDailyGems(
                summary.getSessionDetails().stream()
                        .max(Comparator.comparing(LiveRoomSummaryEntity.SessionDetail::getCreatedOn, Comparator.nullsFirst(Comparator.naturalOrder())))
                        .map(LiveRoomSummaryEntity.SessionDetail::getHostDailyGems)
                        .orElse(0.0)
        );

        summary.setUpdatedOn(
                summary.getSessionDetails().stream()
                        .max(Comparator.comparing(LiveRoomSummaryEntity.SessionDetail::getCreatedOn, Comparator.nullsFirst(Comparator.naturalOrder())))
                        .map(LiveRoomSummaryEntity.SessionDetail::getCreatedOn)
                        .orElse(Instant.now())
        );
    }




}
