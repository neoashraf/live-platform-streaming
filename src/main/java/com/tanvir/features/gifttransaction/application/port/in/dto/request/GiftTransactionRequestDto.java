package com.tanvir.features.gifttransaction.application.port.in.dto.request;

import com.tanvir.features.gifttransaction.domain.valueobjects.SenderReceiverDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GiftTransactionRequestDto {
    private String keycloakId;
//    private String senderMaxId;
    private String senderId;
    private String senderUserType;
//    private String receiverMaxId;
    private String receiverId;
    private String receiverUserType;
    private double beans;
    private double confirmBeans;
    private String transactionType;
    private String transactionId;
    private String category;

//    private String country;
//    private String gender;
//    private String active;
    private String searchKey;
    private Pageable pageable;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    private SenderReceiverDetails senderReceiverDetails;
}
