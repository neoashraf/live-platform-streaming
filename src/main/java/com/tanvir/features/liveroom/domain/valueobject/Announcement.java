package com.tanvir.features.liveroom.domain.valueobject;

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
    private String type;
    private String messageTemplate;
    private AnnouncementUser publisher;
    private AnnouncementUser mentionedUser;
    private Resource resource;
    private Instant time;
}
