package com.tanvir.features.host.adapter.out.persistence.entity;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "host")
public class HostEntity implements Persistable<String> {
    @Id
    private String id;
    private String keycloakId;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
    private LocalDateTime joinedOn;
    private LocalDateTime statusUpdatedAt;

    private String userId; // UUID
    private String maxId; // 8 digit
    private String firstName;
    private String lastName;
    private String displayName;
    private String gender;
    private LocalDate dateOfBirth;
    private String country;
    private String profileImageId;
    private String profileImageUrl;
    private String profileFrameId;
    private String profileFrameUrl;
    private String phoneNoCountryCode;
    private String phoneNo;
    private String email;
    private String active;
    private String userType;

    private String agencyMaxId; // 5 digit
    private String hostType;
    private String nidFrontId;
    private String nidBackId;
    private String nidFrontImageUrl;
    private String nidBackImageUrl;
    private String referredBy;

    private int userLevel;
    private double beans;
    private double gems;
    private double beansGifted;
    private String remarks;
    private String deviceBanned;


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
