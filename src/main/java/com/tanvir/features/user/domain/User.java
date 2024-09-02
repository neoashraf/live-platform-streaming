package com.tanvir.features.user.domain;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;

    private String maxId;
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
    private String password;
    private List<String> roles;
    private String userType;
    private String active;

    private int userLevel;
    private double beans;
    private double gems;

    private String referralId;
    private String profileDescription;
    private String keycloakId;


	@Override
	public String toString() {
		return CommonFunctions.buildGsonBuilder(this);
	}

}
