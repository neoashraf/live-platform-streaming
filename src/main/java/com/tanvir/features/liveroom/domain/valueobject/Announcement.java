package com.tanvir.features.liveroom.domain.valueobject;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Announcement {
    private String announcementId;
    private String type;
    private String messageTemplate;
    private AnnouncementUser publisher;
    private AnnouncementUser mentionedUser;
    private Gift gift;
    private Resource resource;
    private String time;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Gift {
        private int quantity;
        private Resource resource;
        private List<Resources> resources;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Resource {
        private String name;
        private String imageUrl;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Resources {
        private String id;
        private String type;
        private String name;
        private String url;
        private String thumbnailUrl;
    }

    @Override
    public String toString() {
        return CommonFunctions.buildGsonBuilder(this);
    }
}
