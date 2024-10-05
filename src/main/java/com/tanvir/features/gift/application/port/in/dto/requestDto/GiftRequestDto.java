package com.tanvir.features.gift.application.port.in.dto.requestDto;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.level.domain.valueobjects.ResourceFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftRequestDto {

    private String id;
    private String name;
    private String description;
    private String category;
    private int cost;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
    private List<ResourceFormat> resourceFormats;

    private int limit;
    private int offSet;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
