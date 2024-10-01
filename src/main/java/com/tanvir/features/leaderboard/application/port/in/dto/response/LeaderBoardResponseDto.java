package com.tanvir.features.leaderboard.application.port.in.dto.response;

import com.tanvir.features.leaderboard.domain.Leaderboard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaderBoardResponseDto {
    private String message;
    private List<Leaderboard> data;
    private Integer count;
    private boolean error;
}
