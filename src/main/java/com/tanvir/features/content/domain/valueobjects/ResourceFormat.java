package com.tanvir.features.content.domain.valueobjects;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.gift.domain.valueobjects.MetaData;
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
