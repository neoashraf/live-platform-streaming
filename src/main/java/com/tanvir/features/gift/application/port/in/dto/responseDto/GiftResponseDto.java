package com.tanvir.features.gift.application.port.in.dto.responseDto;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.gift.domain.Gift;
import com.tanvir.features.gift.domain.valueobjects.ResourceFormat;
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
public class GiftResponseDto {


    private Object data;
    private String message;
    private boolean error;
    private int count;
    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
