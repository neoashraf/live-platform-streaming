package com.tanvir.features.leaderboard.application.port.in.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardRequestDto {
    private String userId;
    private String agencyMaxId;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private Integer offset;
    private Integer limit;
}
