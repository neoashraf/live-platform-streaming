package com.tanvir.features.host.application.port.in.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HostRequestDto {
    private String id;
    private String userId;
    private String keycloakId;
//    private String hostName;
    private String hostType;
    private String maxId; // 8 digit
    private String agencyMaxId;  // 5 digit
    private String nidFrontId;
    private String nidBackId;
    private String referralId;

    private String nidFrontImageUrl;
    private String nidBackImageUrl;

    private String country;
    private String gender;
    private String active;
    private String searchKey;
    private Pageable pageable;

}
