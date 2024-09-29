package com.tanvir.features.gifttransaction.application.port.in.dto.response;

import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BeanTransactionResponseDto {
    private String message;
    private List<GiftTransaction> data;
    private Integer count;
}
