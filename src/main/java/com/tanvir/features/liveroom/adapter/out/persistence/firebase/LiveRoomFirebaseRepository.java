package com.tanvir.features.liveroom.adapter.out.persistence.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanvir.common.firebase.ReactiveFirebaseRepository;
import com.tanvir.features.liveroom.application.service.FirebaseTimeService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LiveRoomFirebaseRepository extends ReactiveFirebaseRepository<LiveRoomFirebaseEntity> {
    public LiveRoomFirebaseRepository(String referencePath, Class<LiveRoomFirebaseEntity> entityClass, ObjectMapper mapper, FirebaseTimeService firebaseTimeService) {
        super(referencePath, entityClass, mapper, firebaseTimeService);
    }

    /*public LiveRoomFirebaseRepository(@Qualifier("firebasePath") String firebasePath, Class<LiveRoomFirebaseEntity> entityClass) {
        super(firebasePath, entityClass, new ObjectMapper());
    }*/

    public Mono<Void> deleteLiveRoom(String liveRoomId) {
        return this.delete(liveRoomId);
    }
}
