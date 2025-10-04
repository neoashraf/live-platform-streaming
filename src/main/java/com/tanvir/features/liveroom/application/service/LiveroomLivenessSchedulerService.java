package com.tanvir.features.liveroom.application.service;

import com.tanvir.features.liveroom.application.port.out.LiveRoomPersistencePort;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseRepository;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.core.util.enums.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
@ConditionalOnProperty(prefix = "liveroom.liveness.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class LiveroomLivenessSchedulerService {

    private final LiveRoomPersistencePort liveRoomPersistencePort;
    private final LiveRoomFirebaseRepository firebaseRepository;

    @Value("${liveroom.liveness.timeout.minutes:5}")
    private int livenessTimeoutMinutes;

    @Scheduled(fixedRateString = "#{${liveroom.liveness.scheduler.interval.minutes:5} * 60 * 1000}", initialDelay = 60000)
    public void checkAndUpdateOfflineLiverooms() {
        log.info("Running scheduled task to check for offline liverooms...");

        Instant cutoffTime = ZonedDateTime.now(ZoneOffset.UTC).toInstant().minusSeconds(livenessTimeoutMinutes * 60L);

        this.findLiveRoomsToMarkOffline(cutoffTime)
            .flatMap(this::markLiveRoomOffline)
            .doOnNext(liveRoom -> log.info("Marked liveroom {} as offline due to inactivity", liveRoom.getId()))
            .flatMap(this::deleteFromFirebase)
            .doOnNext(liveRoom -> log.info("Deleted liveroom {} from Firebase", liveRoom.getId()))
            .doOnError(error -> log.error("Error in scheduled liveness check: ", error))
            .subscribe(
                liveRoom -> log.debug("Successfully processed offline liveroom: {}", liveRoom.getId()),
                error -> log.error("Error processing offline liverooms: ", error),
                () -> log.info("Completed scheduled liveness check")
            );
    }

    private Flux<LiveRoom> findLiveRoomsToMarkOffline(Instant cutoffTime) {
        return liveRoomPersistencePort.findLiveRoomsForOfflineCheck(cutoffTime)
                .doOnNext(liveRoom -> log.info("Found liveroom {} that hasn't been seen since {}",
                    liveRoom.getId(), liveRoom.getLastSeen()));
    }

    private Mono<LiveRoom> markLiveRoomOffline(LiveRoom liveRoom) {
        liveRoom.setStatus(Constants.STATUS_OFFLINE.getValue());
        liveRoom.setEndedOn(Instant.now());
        return liveRoomPersistencePort.save(liveRoom);
    }

    private Mono<LiveRoom> deleteFromFirebase(LiveRoom liveRoom) {
        return firebaseRepository.deleteLiveRoom(liveRoom.getId())
                .then(Mono.just(liveRoom))
                .doOnError(error -> log.error("Failed to delete liveroom {} from Firebase: {}",
                    liveRoom.getId(), error.getMessage()));
    }
}