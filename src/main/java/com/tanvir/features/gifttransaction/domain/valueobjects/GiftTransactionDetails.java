package com.tanvir.features.gifttransaction.domain.valueobjects;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GiftTransactionDetails {
    private String id;
    private SenderReceiverDetails sender;
    private SenderReceiverDetails receiver;
    private Double beans;
    private String transactionType;
    private String category;
    private LocalDate transactionDate;
    private Instant createdOn;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
