package com.tanvir.features.dailyidgenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ScheduledIdService {

    @Autowired
    private ScheduledIdRepository scheduledIdRepository;

    // Run every day at 1 AM UTC
//    @Scheduled(cron = "0 0 1 * * ?", zone = "UTC")
    @Scheduled(cron = "0 * * * * ?", zone = "UTC")
    public void generateIdAndStore() {
        String newId = UUID.randomUUID().toString();
        ScheduledId scheduledId = new ScheduledId();
        scheduledId.setGeneratedId(newId);
        scheduledId.setCreatedOn(LocalDateTime.now());

        // Save the generated ID in MongoDB
        Mono<ScheduledId> savedId = scheduledIdRepository.save(scheduledId);
        savedId.subscribe();  // Trigger the saving process
    }
}
