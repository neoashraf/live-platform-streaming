package com.tanvir.features.liveroom.application.service;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class FirebaseTimeService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void startTimer(LiveRoomFirebaseEntity entity, DatabaseReference databaseReference) {
        scheduler.scheduleAtFixedRate(() -> {
            if (!"stopped".equals(entity.getStatus())) {
//                entity.setElapsedSeconds(entity.getElapsedSeconds() + 1);

                // Update elapsed time in Firebase
                Map<String, Object> updates = new HashMap<>();
//                updates.put("elapsedSeconds", entity.getElapsedSeconds());

                databaseReference.child(entity.getId()).updateChildren(updates, (databaseError, ref) -> {
                    if (databaseError != null) {
                        // Handle error
                        System.err.println("Failed to update entity: " + databaseError.getMessage());
                    } else {
                        // Timer successfully updated
                        System.out.println("Timer updated successfully");
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopTimer() {
        scheduler.shutdownNow();
    }

    public void startTimerV2(LiveRoomFirebaseEntity entity, DatabaseReference databaseReference) {
        scheduler.scheduleAtFixedRate(() -> {
            if (!"stopped".equals(entity.getStatus())) {
//                entity.setElapsedSeconds(entity.getElapsedSeconds() + 1);

                // Convert elapsedSeconds to hh:mm:ss format
                /*String formattedTime = String.format("%02d:%02d:%02d",
                        TimeUnit.SECONDS.toHours(entity.getElapsedSeconds()),
                        TimeUnit.SECONDS.toMinutes(entity.getElapsedSeconds()) % 60,
                        entity.getElapsedSeconds() % 60);*/

                // Update both elapsedSeconds and formattedTime in Firebase
                Map<String, Object> updates = new HashMap<>();
//                updates.put("elapsedSeconds", entity.getElapsedSeconds());
//                updates.put("formattedTime", formattedTime);

                databaseReference.child(entity.getId()).updateChildren(updates, (databaseError, ref) -> {
                    if (databaseError != null) {
                        // Handle error
                        System.err.println("Failed to update entity: " + databaseError.getMessage());
                    } else {
                        // Timer successfully updated
//                        System.out.println("Timer updated successfully: " + formattedTime);
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
