package com.tanvir.features.liveroom.adapter.out.persistence.firebase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveRoomConfig {

    @Bean
    public Class<LiveRoomFirebaseEntity> teenPattiGameEntityClass() {
        return LiveRoomFirebaseEntity.class;
    }

    @Bean
    public String firebasePath(@Value("${firebase.path}") String firebasePath) {
        return firebasePath;
    }
}
