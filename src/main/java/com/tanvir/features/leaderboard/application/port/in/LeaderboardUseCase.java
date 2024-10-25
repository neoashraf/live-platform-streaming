package com.tanvir.features.leaderboard.application.port.in;

import com.tanvir.features.leaderboard.application.port.in.dto.request.LeaderboardRequestDto;
import com.tanvir.features.leaderboard.application.port.in.dto.response.LeaderBoardResponseDto;
import reactor.core.publisher.Mono;

public interface LeaderboardUseCase {
    Mono<LeaderBoardResponseDto> getFanLeaderBoard(LeaderboardRequestDto requestDto);
    Mono<LeaderBoardResponseDto> getHostLeaderBoard(LeaderboardRequestDto requestDto);
    Mono<LeaderBoardResponseDto> getAgencyLeaderBoard(LeaderboardRequestDto requestDto);
}
