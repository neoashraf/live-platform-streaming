package com.tanvir.features.gifttransaction.adapter.out.persistence.entity;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "gift_transactions")
public class GiftTransactionEntity implements Persistable<String> {
    @Id
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

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isNew() {
        boolean isNull = Objects.isNull(this.id);
        this.id = isNull ? UUID.randomUUID().toString() : this.id;
        return isNull;
    }
}

