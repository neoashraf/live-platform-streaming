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

    private List<String> tags;
    private Integer viewers;

//  new proposed
    private String id;


    private LocalDateTime createdOn;
    private LocalDateTime endedOn;


    private String thumbnailId;
    private String thumbnailUrl;


    private String title;
    private String welcomeNote;
    private List<String> tag;
    private String type;
    private String status;
    private Long duration;
    private String hostUserId;
    private List<String> kickedOutUserIds;
    private long dailyReceivedGems;


    private Integer levelCompletionPercentage; // no need (only needed in response)




    // host related info
    private String userId;
    private String keycloakId;
    private String country;
    private String profilePicture; // profilePictureUrl
    private Integer starCount;
    private Long gemsCount;
    private Long beansCount;
    private String maxId;
    private String gender;
    private String profilePictureUrl;

    private Integer popularityLevel; // no need
    private Integer userLevel;




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
