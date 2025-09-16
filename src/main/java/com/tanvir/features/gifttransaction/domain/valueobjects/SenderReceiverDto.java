package com.tanvir.features.gifttransaction.domain.valueobjects;

import com.tanvir.features.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SenderReceiverDto {
    private User sender;
    private User receiver;
    private List<User> receivers;
    private Map<String, Object> senderUpdatedFields;
    private Map<String, Object> receiverUpdatedFields;
}
