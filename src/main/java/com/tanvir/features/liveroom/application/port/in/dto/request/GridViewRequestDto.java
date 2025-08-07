package com.tanvir.features.liveroom.application.port.in.dto.request;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GridViewRequestDto {
    private String viewMode;
    private String country;
    private String mediaType;
    private Pageable pageable;
    private String keycloakId;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
