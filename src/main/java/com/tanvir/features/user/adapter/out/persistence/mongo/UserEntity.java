package com.tanvir.features.user.adapter.out.persistence.mongo;
import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "users")
public class UserEntity {

    @Id
    private String id;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;

    @Indexed(unique = true)
    private String maxId;
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
    @Indexed(unique = true)
    private String email;
    private String passwordCreated;
    private String password;
    private List<String> roles;
    private String userType;
    private String active;

    private int userLevel;
    private double beans;
    private double gems;

    private String profileDescription;
    @Indexed(unique = true)
    private String keycloakId;



    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }

}
