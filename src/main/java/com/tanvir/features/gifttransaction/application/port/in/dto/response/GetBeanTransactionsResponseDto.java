package com.tanvir.features.gifttransaction.application.port.in.dto.response;

import com.tanvir.features.gifttransaction.domain.valueobjects.GiftTransactionDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetBeanTransactionsResponseDto {
    private String message;
    private List<GiftTransactionDetails> data;
    private Integer count;
}
