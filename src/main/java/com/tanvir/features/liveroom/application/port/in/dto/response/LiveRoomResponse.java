package com.tanvir.features.liveroom.application.port.in.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomResponse {
    private String id;
    private String thumbnailId;
    private String thumbnailUrl;
    private String title;
    private String type;
    private List<String> tags;
    private String status;
    private long viewerCount;
    private Instant createdOn;
    private String country;
}
