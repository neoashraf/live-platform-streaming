package com.tanvir.features.level.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "levels")
public class LevelEntity {
    @Id
    private String id;
    private int level;
    private String levelBadgeId;
    private String levelBadgeUrl;
    private long nextLevelExpTargetValue;
    private String nextLevelExpTargetName;
}
