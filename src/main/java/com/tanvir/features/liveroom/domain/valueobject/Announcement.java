package com.tanvir.features.liveroom.domain.valueobject;

import com.tanvir.core.util.CommonFunctions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Announcement {
    private String liveRoomId;
    private String announcementId;
    private String type;
    private String messageTemplate;
    private AnnouncementUser publisher;
    private AnnouncementUser mentionedUser;
    private AnnouncementUser receiverUser;
    private Gift gift;
    private Resource resource;
    private Ride ride;
    private String time;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Gift {
        private String id;
        private String name;
        private int quantity;
        private Resource resource;
        private List<Resources> resources;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ride {
        private String id;
        private String name;
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
