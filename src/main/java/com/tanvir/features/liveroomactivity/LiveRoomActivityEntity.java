package com.tanvir.features.liveroomactivity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "liveroom_activity")
public class LiveRoomActivityEntity {
    @Id
    private String id;
    private String userId;
    private Double dailyReceivedGems;
    private Instant endTime;
    private Instant createdOn;
    private Instant updatedOn;
    private Instant resetOn;
//    private Instant endTimeZoned;
}
