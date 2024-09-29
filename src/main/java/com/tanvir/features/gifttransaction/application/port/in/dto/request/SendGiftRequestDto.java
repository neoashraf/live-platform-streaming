package com.tanvir.features.gifttransaction.application.port.in.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendGiftRequestDto {
    private String keycloakId;
    private String senderId;
    private String receiverId;
    private String giftId;
    private int quantity;
    private String liveSession;
    private String liveRoomId;
}
