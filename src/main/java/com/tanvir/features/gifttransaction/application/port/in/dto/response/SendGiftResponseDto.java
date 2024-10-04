package com.tanvir.features.gifttransaction.application.port.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendGiftResponseDto {
    private String message;
    private DataDto data;
    private int count;
    private boolean error;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataDto {
        private String giftId;
        private String recipientId;
        private String senderId;
        private int senderLevel;
        private int quantity;
        private String sentOn;
    }
}
