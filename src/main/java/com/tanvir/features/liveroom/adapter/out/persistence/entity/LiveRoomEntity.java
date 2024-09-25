package com.tanvir.features.liveroom.adapter.out.persistence.entity;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "liverooms")
public class LiveRoomEntity implements Persistable<String> {

    private String id;
    private LocalDateTime createdOn;
    private LocalDateTime endedOn;
    private String thumbnailId;
    private String thumbnailUrl;

    private String title;
    private String description;
    private List<String> tags;
    private String type;
    private String status;
    private String country;
    private String hostId;
    private List<String> kickedOutUserIds;
    private long viewerCount;
    private long dailyReceivedGems;

    private long duration;


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
