package com.tanvir.features.gifttransaction.domain.valueobjects;

import com.tanvir.features.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SenderReceiverDto {
    private User sender;
    private User receiver;
}
