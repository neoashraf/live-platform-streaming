package com.tanvir.features.commonbusiness.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceUrlDto {
    private String resourceUrl;
    private String thumbnailUrl;
}
