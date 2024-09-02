package com.tanvir.features.user.application.port.in.dto.request;

import com.tanvir.core.util.CommonFunctions;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserRequestDTO {

	private String userId;
	private String id;
	private String maxId;
	private String keycloakId;
	private String firstName;
	private String lastName;
	private String displayName;
	private String profilePicture;
	private LocalDate dateOfBirth;
	private int userLevel;
	private String role;
	private String isHost;
	private String isLive;
	private String gender;
	private String country;
	private String active;
	private String searchKey;
	private LocalDateTime createdAt;
	private int limit;
	private int offSet;
	private String newRole;
	private String profileImageId;
	private String profileImageUrl;
	private String profileDescription;


	@Override
	public String toString() {
		return CommonFunctions.buildGsonBuilder(this);
	}


}
