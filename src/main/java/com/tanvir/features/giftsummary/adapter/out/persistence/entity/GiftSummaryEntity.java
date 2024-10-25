package com.tanvir.features.giftsummary.adapter.out.persistence.entity;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "gift_summary")
public class GiftSummaryEntity implements Persistable<String> {
    @Id
    private String id;
    private List<String> giftTransactionIds;
    private String userId;
    private String agencyId;
    private Double beans;
    private Double gems; // determine beans or gems
    private String transactionDateId; // a unique id to represent a particular date
    private String transactionDate;
    private Integer transactionCount;
    private Instant createdOn;
    private Instant updatedOn;
    private Map<String, Double> senderAmountMap;

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
