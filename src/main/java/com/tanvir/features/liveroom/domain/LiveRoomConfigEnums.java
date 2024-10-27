package com.tanvir.features.liveroom.domain;

import lombok.Getter;

@Getter
public enum LiveRoomConfigEnums {

    maxAudioParticipants(8);

    private final Integer value;

    LiveRoomConfigEnums(Integer value) {
        this.value = value;
    }
}
