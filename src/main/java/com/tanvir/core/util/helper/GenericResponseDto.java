package com.tanvir.core.util.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponseDto {
    private String message;
    private List<Object> data;
    private Integer count;
    private boolean error;
}
