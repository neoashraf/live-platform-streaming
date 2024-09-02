package com.tanvir.features.liveroom.application.port.in.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GridViewRequestDto {
    private String tab;
    private String country;
    private Integer offset;
    private Integer limit;
}
