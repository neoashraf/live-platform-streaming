package com.tanvir.features.liveroom.application.port.in.dto.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeatNumberDto {
    private boolean availableStatus;
    private String userId;
    private String joinReqId;
}
