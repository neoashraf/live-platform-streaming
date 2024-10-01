package com.tanvir.features.leaderboard.application.service;

import com.tanvir.features.giftsummary.application.port.in.GiftSummaryUseCase;
import com.tanvir.features.giftsummary.domain.GiftSummary;
import com.tanvir.features.leaderboard.application.port.in.LeaderboardUseCase;
import com.tanvir.features.leaderboard.application.port.in.dto.request.LeaderboardRequestDto;
import com.tanvir.features.leaderboard.application.port.in.dto.response.LeaderBoardResponseDto;
import com.tanvir.features.leaderboard.domain.Leaderboard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaderboardService implements LeaderboardUseCase {

    private final GiftSummaryUseCase giftSummaryUseCase;

    public LeaderboardService(GiftSummaryUseCase giftSummaryUseCase) {
        this.giftSummaryUseCase = giftSummaryUseCase;
    }

    @Override
    public Mono<LeaderBoardResponseDto> getFanLeaderBoard(LeaderboardRequestDto requestDto) {
        return giftSummaryUseCase.getGiftSummaryByUserIdAndDate(requestDto.getUserId(), requestDto.getCreatedAfter(), requestDto.getCreatedBefore())
                .flatMapMany(Flux::fromIterable)
                .map(GiftSummary::getSenderAmountMap)
                // Flatten the Flux of Maps to individual senderId and amount entries
                .flatMap(map -> Flux.fromIterable(map.entrySet()))
                // Accumulate the total amount per sender in a Map
                .collect(HashMap<String, Double>::new, (accumulatedMap, entry) ->
                        accumulatedMap.merge(entry.getKey(), entry.getValue(), Double::sum))
                // Once all amounts are accumulated, sort by the values (amount) in descending order
                .map(totalAmountMap -> {
                    // Step 1: Convert the map entries to a list
                    List<Map.Entry<String, Double>> entryList = new ArrayList<>(totalAmountMap.entrySet());

                    // Step 2: Sort the list by the value (amount) in descending order
                    entryList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

                    // Step 3: Limit the list to the top 'n' entries (you can pass 'n' as a parameter)
                    int n = requestDto.getLimit();  // Assuming 'n' is passed in the request DTO
                    List<Map.Entry<String, Double>> limitedList = entryList.stream()
                            .limit(n)
                            .toList();

                    // Step 4: Transform the limited list of entries to Leaderboard objects
                    List<Leaderboard> leaderboardList = limitedList.stream()
                            .map(entry -> {
                                Leaderboard leaderboard = new Leaderboard();
                                leaderboard.setUserId(entry.getKey());
                                leaderboard.setBeansSent(entry.getValue());
                                return leaderboard;
                            })
                            .collect(Collectors.toList());

                    return leaderboardList;
                })
                .map(leaderboardList -> {
                    // Create and return the LeaderBoardResponseDto
                    LeaderBoardResponseDto responseDto = new LeaderBoardResponseDto();
                    responseDto.setMessage("Fan Leaderboard fetched successfully");
                    responseDto.setData(leaderboardList);
                    responseDto.setCount(leaderboardList.size());
                    responseDto.setError(false);

                    return responseDto;
                });
    }
}
