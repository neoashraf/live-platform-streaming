package com.tanvir.features.content.domain;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.gift.domain.valueobjects.ResourceFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Content {
    private String id;
    private String name;
    private String description;
    private String type;
    //    private String category;
    private double cost;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
    private List<ResourceFormat> resourceFormats;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
