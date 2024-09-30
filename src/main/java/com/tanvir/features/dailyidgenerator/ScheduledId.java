package com.tanvir.features.dailyidgenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "scheduled_ids")
public class ScheduledId {

    @Id
    private String id;
    private String generatedId;
    private LocalDateTime createdOn;

    // getters and setters
}
