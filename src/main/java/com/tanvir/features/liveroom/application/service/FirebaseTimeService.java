package com.tanvir.features.liveroom.application.service;

import com.google.firebase.database.DatabaseReference;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class FirebaseTimeService {
    private Disposable timerDisposable;

    public void startTimer(LiveRoomFirebaseEntity entity, DatabaseReference databaseReference) {
        stopTimer(); // Stop any previous timer
        timerDisposable = Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
            .takeUntilOther(Flux.create(sink -> {
                if ("stopped".equals(entity.getStatus())) {
                    sink.complete();
                }
            }))
            .subscribe(tick -> {
                // Non-blocking timer tick
                Map<String, Object> updates = new HashMap<>();
                // updates.put("elapsedSeconds", entity.getElapsedSeconds());
                databaseReference.child(entity.getId()).updateChildren(updates, (databaseError, ref) -> {
                    if (databaseError != null) {
                        System.err.println("Failed to update entity: " + databaseError.getMessage());
                    } else {
                        System.out.println("Timer updated successfully");
                    }
                });
            });
    }

    public void stopTimer() {
        if (timerDisposable != null && !timerDisposable.isDisposed()) {
            timerDisposable.dispose();
        }
    }

    public void startTimerV2(LiveRoomFirebaseEntity entity, DatabaseReference databaseReference) {
        stopTimer(); // Stop any previous timer
        timerDisposable = Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
            .takeUntilOther(Flux.create(sink -> {
                if ("stopped".equals(entity.getStatus())) {
                    sink.complete();
                }
            }))
            .subscribe(tick -> {
                // Non-blocking timer tick
                Map<String, Object> updates = new HashMap<>();
                // updates.put("elapsedSeconds", entity.getElapsedSeconds());
                // updates.put("formattedTime", formattedTime);
                databaseReference.child(entity.getId()).updateChildren(updates, (databaseError, ref) -> {
                    if (databaseError != null) {
                        System.err.println("Failed to update entity: " + databaseError.getMessage());
                    } else {
                        // System.out.println("Timer updated successfully: " + formattedTime);
                    }
                });
            });
    }
}
