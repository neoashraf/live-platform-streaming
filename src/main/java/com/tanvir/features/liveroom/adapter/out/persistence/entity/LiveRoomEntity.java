package com.tanvir.features.liveroom.adapter.out.persistence.entity;

import com.tanvir.core.util.CommonFunctions;
import com.tanvir.features.liveroom.domain.valueobject.Fan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "liverooms")
public class LiveRoomEntity implements Persistable<String> {
    private String id;
    private String userId;
    private String keycloakId;
    private String country;
    private String profilePicture; // profilePictureUrl
    private String name;
    private String welcomeNote;
    private Integer starCount;
    private Long gemsCount;
    private Long beansCount;
    private String type;
    private String tag; // array
    private Integer fansCount;
    private String isLive;
    private Integer popularityLevel; // no need
    private Integer userLevel;
    private Map<String, Fan> fans;
    private List<String> kickedOutUsers;
    private Long duration;
    private Integer levelCompletionPercentage; // no need (only needed in response)

    private String createdBy; // no need
    private LocalDateTime createdOn;
    private String endedBy; // no need
    private LocalDateTime endedOn;

    private String maxId;
    private String gender;
    private String profilePictureUrl;
    private long dailyReceivedGems;



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
