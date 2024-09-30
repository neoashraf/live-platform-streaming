package com.tanvir.features.giftsummary.domain;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GiftSummary {
    private String id;
    private List<String> giftTransactionIds;
    private String userId;
    private Double beans;
    private Double gems; // determine beans or gems
    private String transactionDateId; // a unique id to represent a particular date
    private String transactionDate;
    private Integer transactionCount;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private Map<String, Double> senderAmountMap;

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
