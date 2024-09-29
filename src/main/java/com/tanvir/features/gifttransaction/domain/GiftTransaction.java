package com.tanvir.features.gifttransaction.domain;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.gift.domain.Gift;
import com.tanvir.features.gifttransaction.domain.valueobjects.SenderReceiverDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GiftTransaction {
    private String id;
    private String senderId;
    private String receiverId;
    private String giftId;
    private Integer quantity;
    private Double beans;
    private String liveSession;
    private String liveRoomId;
    private String transactionDateId;
    private LocalDate transactionDate;
    private LocalDateTime createdOn;

    // dto
    private SenderReceiverDto senderReceiverDto;
    private Gift gift;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
