package com.tanvir.features.liveroom.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomDataDto {
    private String id;
    private Instant joinedOn;
    private Instant leftOn;
    private Instant endedOn;
    private Announcement announcement;
}
