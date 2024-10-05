package com.tanvir.features.gift.application.port.in.dto.responseDto;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
