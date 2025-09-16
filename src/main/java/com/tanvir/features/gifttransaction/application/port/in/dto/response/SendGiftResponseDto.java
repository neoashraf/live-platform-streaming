package com.tanvir.features.gifttransaction.application.port.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendGiftResponseDto {
    private String message;
    private DataDto data;
    private List<DataDto> giftData;
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
        private String levelBadgeUrl;
        private int quantity;
        private String sentOn;
        private double beans;
        private String beansValue;
    }
}
