package com.tanvir.features.metaproperty.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetaProperty {
    private Integer popularIndex;
    private Integer starIndex;
    private Double gemsConversionRate;
}
