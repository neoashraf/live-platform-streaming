package com.tanvir.features.liveroom.domain.valueobject;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnableJoinPermission {
    private String roomId;
    private String joinCallAvailable;
    private String autoJoinAudioStreamAvailable;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
