package com.tanvir.features.leaderboard.application.port.in;

import com.tanvir.features.leaderboard.application.port.in.dto.request.LeaderboardRequestDto;
import com.tanvir.features.leaderboard.application.port.in.dto.response.LeaderBoardResponseDto;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LeaderboardUseCase {
    Mono<LeaderBoardResponseDto> getFanLeaderBoard(LeaderboardRequestDto requestDto);
    Mono<LeaderBoardResponseDto> getHostLeaderBoard(LeaderboardRequestDto requestDto);
    Mono<LeaderBoardResponseDto> getAgencyLeaderBoard(LeaderboardRequestDto requestDto);
}
