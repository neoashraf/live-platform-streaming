package com.tanvir.features.level.adapter.out.persistence.entity;

import com.tanvir.features.level.domain.valueobjects.ResourceFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "levels")
public class LevelEntity {
    @Id
    private String id;
    private int level;
    private List<ResourceFormat> resourceFormats;
    private long nextLevelExpTargetValue;
    private String nextLevelExpTargetName;
}
