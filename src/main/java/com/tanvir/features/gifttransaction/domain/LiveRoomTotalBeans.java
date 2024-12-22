package com.tanvir.features.gifttransaction.domain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomTotalBeans {
    private String liveRoomId;
    private String receiverId;
    private Double totalBeans;
}
