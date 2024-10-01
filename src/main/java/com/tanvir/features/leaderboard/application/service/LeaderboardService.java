package com.tanvir.features.leaderboard.application.service;

import com.tanvir.features.giftsummary.application.port.in.GiftSummaryUseCase;
import com.tanvir.features.giftsummary.domain.GiftSummary;
import com.tanvir.features.leaderboard.application.port.in.LeaderboardUseCase;
import com.tanvir.features.leaderboard.application.port.in.dto.request.LeaderboardRequestDto;
import com.tanvir.features.leaderboard.application.port.in.dto.response.LeaderBoardResponseDto;
import com.tanvir.features.leaderboard.domain.GiftSummaryUser;
import com.tanvir.features.leaderboard.domain.Leaderboard;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import com.tanvir.features.user.application.port.in.UserUseCase;
import com.tanvir.features.user.domain.User;
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
    private final UserUseCase userUseCase;

    public LeaderboardService(GiftSummaryUseCase giftSummaryUseCase, UserUseCase userUseCase) {
        this.giftSummaryUseCase = giftSummaryUseCase;
        this.userUseCase = userUseCase;
    }

    @Override
    public Mono<LeaderBoardResponseDto> getFanLeaderBoard(LeaderboardRequestDto requestDto) {
        return giftSummaryUseCase.getGiftSummaryByUserIdAndDate(
                        requestDto.getUserId(), requestDto.getCreatedAfter(), requestDto.getCreatedBefore())
                .flatMapMany(Flux::fromIterable)
                .map(GiftSummary::getSenderAmountMap)
                // Flatten the Flux of Maps to individual senderId and amount entries
                .flatMap(map -> Flux.fromIterable(map.entrySet()))
                // Accumulate the total amount per sender in a Map
                .collect(HashMap<String, Double>::new, (accumulatedMap, entry) ->
                        accumulatedMap.merge(entry.getKey(), entry.getValue(), Double::sum))
                // Once all amounts are accumulated, sort by the values (amount) in descending order
                .flatMap(totalAmountMap -> {
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
                    List<String> userIdList = limitedList.stream()
                            .map(Map.Entry::getKey)
                            .toList();

                    // Fetch user details using userUseCase.getUsersByIds(userIdList) and return combined result
                    return userUseCase.getUsersByIds(userIdList)
                            .doOnError(throwable -> log.error("Error while fetching users by ids: {}", throwable.getMessage()))
                            .map(stringUserMap -> {
                                List<GiftSummaryUser> summaryUserList = new ArrayList<>();

                                limitedList.forEach(entry -> {
                                    GiftSummaryUser giftSummaryUser = new GiftSummaryUser();

                                    // Fetch user details from the stringUserMap using the entry's key (userId)
                                    String userId = entry.getKey();
                                    User user = stringUserMap.get(userId);

                                    if (user != null) {
                                        giftSummaryUser.setUserId(userId);
                                        giftSummaryUser.setDisplayName(user.getDisplayName());
                                        giftSummaryUser.setProfileImageUrl(user.getProfileImageUrl());
                                        giftSummaryUser.setUserLevel(user.getUserLevel());
                                        giftSummaryUser.setBeansSent(entry.getValue()); // Correctly set the beansSent value from the entry's value
                                    }

                                    summaryUserList.add(giftSummaryUser);
                                });
                                return summaryUserList;
                            });
                })
                .zipWith(giftSummaryUseCase
                        .getTotalGiftAmountByUserIdAndDate(requestDto.getUserId(), requestDto.getCreatedAfter(), requestDto.getCreatedBefore())
                        .doOnNext(totalGiftAmount -> log.info("Total gift amount: {}", totalGiftAmount)))
                .map(userListAndTotalGiftAmountTuple -> {
                    List<GiftSummaryUser> giftSummaryUsers = userListAndTotalGiftAmountTuple.getT1();
                    Double totalGiftAmount = userListAndTotalGiftAmountTuple.getT2();
                    LeaderBoardResponseDto responseDto = new LeaderBoardResponseDto();
                    Leaderboard leaderboard = Leaderboard
                            .builder()
                            .topGiftSenders(giftSummaryUsers)
                            .totalGiftAmount(totalGiftAmount)
                            .build();
                    responseDto.setData(leaderboard);
                    responseDto.setMessage("Fan Leaderboard fetched successfully");
                    return responseDto;
                });
    }


    @Override
    public Mono<List<UserBeanSummary>> getHostLeaderBoard(LeaderboardRequestDto requestDto) {
        return giftSummaryUseCase.getHostGiftSummariesByDate(requestDto.getCreatedAfter(), requestDto.getCreatedBefore());
    }
}
