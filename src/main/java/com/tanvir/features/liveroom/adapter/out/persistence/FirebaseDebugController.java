package com.tanvir.features.liveroom.adapter.out.persistence;

import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/streaming/debug/firebase")
public class FirebaseDebugController {
    private final FirebaseAdapter firebaseAdapter;

    public FirebaseDebugController(FirebaseAdapter firebaseAdapter) {
        this.firebaseAdapter = firebaseAdapter;
    }

    @PostMapping("/create")
    public Mono<LiveRoomFirebaseEntity> createLiveRoom(@RequestBody LiveRoomFirebaseEntity entity) {
        return firebaseAdapter.create(entity);
    }
}
