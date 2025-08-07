package com.tanvir.features.content.adapter.out.persistence.entity;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "bag_items")
public class BagEntity {
    @Id
    private String id;
    private String contentId;
    private String userId;
    private String type;
    private Instant createdOn;
    private String createdBy;
    private String active;
    private String source;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}