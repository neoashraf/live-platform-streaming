package com.tanvir.features.host.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Host {
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
    private String gender;   // enum -> Male, Female, Others
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
    private String levelBadgeUrl;
    private double beans;
    private double gems;
    private double beansGifted;
    private String remarks;

}
