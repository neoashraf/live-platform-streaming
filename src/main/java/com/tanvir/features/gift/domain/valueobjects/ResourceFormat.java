package com.tanvir.features.gift.domain.valueobjects;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceFormat {
    private String resourceType;
    private String resourceId;
    private String thumbnailId;
    private String resourceUrl;
    private String thumbnailUrl;
    private Map<String, MetaData> metadata;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
