package com.tanvir.features.liveroomsummary.adapter.in.web.router;


import com.tanvir.features.liveroomsummary.application.port.in.LiveRoomSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/streaming")
public class LiveRoomSummaryController {

    @Autowired
    private final LiveRoomSummaryUseCase liveRoomSummaryUseCase;


    @GetMapping("/update")
    public ResponseEntity<String> updateLiveRoomSummary() {
        try {
            // Trigger the update process (non-reactive)
            liveRoomSummaryUseCase.updateLiveRoomSummary();

            // Return a successful response
            return ResponseEntity.ok("Live room summary update initiated.");
        } catch (Exception e) {
            // Handle errors and return appropriate responses
            return ResponseEntity.status(500).body("An error occurred while updating live room summary: " + e.getMessage());
        }
    }
}
