package com.tanvir.features.gifttransaction.domain.valueobjects;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SenderReceiverDetails {
    private String maxId;
    private String userId;
    private String userType;
    private String displayName;
    private String profileImageId;
    private String profileImageUrl;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
