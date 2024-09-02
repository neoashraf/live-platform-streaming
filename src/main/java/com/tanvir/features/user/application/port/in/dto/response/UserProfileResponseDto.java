package com.tanvir.features.user.application.port.in.dto.response;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserProfileResponseDto {

    private String message;
    private User data;
    private int count;
    private boolean error;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }


}
