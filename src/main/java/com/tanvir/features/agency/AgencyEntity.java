package com.tanvir.features.agency;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@Document(collection = "agency")
public class AgencyEntity implements Persistable<String> {
    @Id
    private String id;
    private String keycloakId;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
    private String userId; // UUID
    private String maxId; // 5 digit
    private String firstName;
    private String lastName;
    private String displayName;
    private String gender;
    private LocalDate dateOfBirth;
    private String country;
    private String profileImageId;
    private String profileImageUrl;
    private String phoneNoCountryCode;
    private String phoneNo;
    private String email;
    private String passwordCreated;

    //    private String password;
    private String role;
    private String active;
    private String status;
    private String agencyName;
    private String holderName;
    private String nidFrontId;
    private String nidBackId;
    private String nidFrontImageUrl;
    private String nidBackImageUrl;
    private String referredBy;
    private LocalDate agencySince;
    private String userType;

    //    private LocalDateTime appliedOn;
    private LocalDateTime approvedOn;
    private String remarks;

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