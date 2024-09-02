package com.tanvir.features.liveroom.adapter.out.persistence.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tanvir.common.firebase.ReactiveFirebaseRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LiveRoomFirebaseRepository extends ReactiveFirebaseRepository<LiveRoomFirebaseEntity> {

    public LiveRoomFirebaseRepository(@Qualifier("firebasePath") String firebasePath, Class<LiveRoomFirebaseEntity> entityClass) {
        super(firebasePath, entityClass, new ObjectMapper());
    }
}
