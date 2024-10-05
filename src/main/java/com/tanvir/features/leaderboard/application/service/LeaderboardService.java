package com.tanvir.features.leaderboard.application.service;

import com.tanvir.core.util.enums.ResourceTypeEnum;
import com.tanvir.features.agency.AgencyEntity;
import com.tanvir.features.agency.AgencyService;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.giftsummary.application.port.in.GiftSummaryUseCase;
import com.tanvir.features.giftsummary.domain.GiftSummary;
import com.tanvir.features.leaderboard.application.port.in.LeaderboardUseCase;
import com.tanvir.features.leaderboard.application.port.in.dto.request.LeaderboardRequestDto;
import com.tanvir.features.leaderboard.application.port.in.dto.response.LeaderBoardResponseDto;
import com.tanvir.features.leaderboard.domain.GiftSummaryUser;
import com.tanvir.features.leaderboard.domain.Leaderboard;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.level.domain.Level;
import com.tanvir.features.level.domain.valueobjects.ResourceFormat;
import com.tanvir.features.user.application.port.in.UserUseCase;
import com.tanvir.features.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaderboardService implements LeaderboardUseCase {

    private final GiftSummaryUseCase giftSummaryUseCase;
    private final UserUseCase userUseCase;
    private final AgencyService agencyService;
    private final LevelUseCase levelUseCase;

    public LeaderboardService(GiftSummaryUseCase giftSummaryUseCase, UserUseCase userUseCase, AgencyService agencyService, LevelUseCase levelUseCase) {
        this.giftSummaryUseCase = giftSummaryUseCase;
        this.userUseCase = userUseCase;
        this.agencyService = agencyService;
        this.levelUseCase = levelUseCase;
    }

    @Override
    public Mono<LeaderBoardResponseDto> getFanLeaderBoard(LeaderboardRequestDto requestDto) {
        if (requestDto.getLimit() == null || requestDto.getLimit() == 0) {
            requestDto.setLimit(10);  // Set a default limit if not provided
        }
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
                            .flatMap(stringUserMap -> {
                                List<Integer> levels = stringUserMap.values().stream().map(User::getUserLevel).toList();
                                return levelUseCase.getLevelDomains(levels)
                                        .collect(Collectors.toMap(Level::getLevel, level -> level))
                                        .map(integerLevelMap -> Tuples.of(stringUserMap, integerLevelMap));
                })
                .map(tuple2 -> {
                    Map<String, User> stringUserMap = tuple2.getT1();
                    Map<Integer, Level> integerLevelMap = tuple2.getT2();
                    List<GiftSummaryUser> summaryUserList = new ArrayList<>();

                    limitedList.forEach(entry -> {
                        GiftSummaryUser giftSummaryUser = new GiftSummaryUser();

                        // Fetch user details from the stringUserMap using the entry's key (userId)
                        String userId = entry.getKey();
                        User user = stringUserMap.get(userId);
                        Level userLevel = integerLevelMap.get(user.getUserLevel());
                        log.info("User level: {}", userLevel);
                        ResourceFormat levelResource = CommonBusiness.getResourceFormatByResourceType(
                                userLevel.getResourceFormats(),
                                ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                        giftSummaryUser.setUserId(userId);
                        giftSummaryUser.setDisplayName(user.getDisplayName());
                        giftSummaryUser.setProfileImageUrl(user.getProfileImageUrl());
                        giftSummaryUser.setUserLevel(user.getUserLevel());
                        giftSummaryUser.setLevelUrl(levelResource.getResourceUrl());
                        giftSummaryUser.setProfileFrameUrl(user.getProfileFrameUrl());
                        giftSummaryUser.setBeansSent(entry.getValue()); // Correctly set the beansSent value from the entry's value

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
    public Mono<LeaderBoardResponseDto> getHostLeaderBoard(LeaderboardRequestDto requestDto) {
        if (requestDto.getLimit() == null || requestDto.getLimit() == 0) {
            requestDto.setLimit(10);  // Set a default limit if not provided
        }
        return giftSummaryUseCase.getHostGiftSummariesByDate(requestDto.getCreatedAfter(), requestDto.getCreatedBefore(), requestDto.getLimit())
                .flatMap(userBeanSummaries -> {
                    // Map userId to total beans
                    Map<String, Double> userIdToBeansMap = userBeanSummaries.stream()
                            .collect(Collectors.toMap(UserBeanSummary::getId, UserBeanSummary::getTotalBeans));

                    // Extract user IDs from the userBeanSummaries list
                    List<String> userIdList = userBeanSummaries.stream()
                            .map(UserBeanSummary::getId)
                            .toList();

                    // Fetch user details based on the extracted userIdList
                    return userUseCase.getUsersByIds(userIdList)
                            .doOnError(throwable -> log.error("Error while fetching users by ids: {}", throwable.getMessage()))
                            .flatMap(stringUserMap -> {
                                        List<Integer> levels = stringUserMap.values().stream().map(User::getUserLevel).toList();
                                        return levelUseCase.getLevelDomains(levels)
                                                .collect(Collectors.toMap(Level::getLevel, level -> level))
                                                .map(integerLevelMap -> Tuples.of(stringUserMap, integerLevelMap));
                                    })
                            .map(tuple2 -> {
                                Map<String, User> stringUserMap = tuple2.getT1();
                                Map<Integer, Level> integerLevelMap = tuple2.getT2();
                                List<GiftSummaryUser> summaryUserList = new ArrayList<>();

                                userIdList.forEach(userId -> {
                                    GiftSummaryUser giftSummaryUser = new GiftSummaryUser();

                                    // Fetch user details from the stringUserMap
                                    User user = stringUserMap.get(userId);
                                    Level userLevel = integerLevelMap.get(user.getUserLevel());
                                    ResourceFormat levelResource = CommonBusiness.getResourceFormatByResourceType(
                                            userLevel.getResourceFormats(),
                                            ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                                    giftSummaryUser.setUserId(userId);
                                    giftSummaryUser.setDisplayName(user.getDisplayName());
                                    giftSummaryUser.setProfileImageUrl(user.getProfileImageUrl());
                                    giftSummaryUser.setUserLevel(user.getUserLevel());
                                    giftSummaryUser.setLevelUrl(levelResource.getResourceUrl());
                                    giftSummaryUser.setProfileFrameUrl(user.getProfileFrameUrl());
                                    giftSummaryUser.setBeansReceived(userIdToBeansMap.get(userId));  // Set the total beans

                                    summaryUserList.add(giftSummaryUser);
                                });

                                // Return the list of GiftSummaryUsers
                                return summaryUserList;
                                });
                })
                .map(summaryUserList -> {
                    // Create the LeaderBoardResponseDto
                    LeaderBoardResponseDto responseDto = new LeaderBoardResponseDto();

                    Leaderboard leaderboard = Leaderboard
                            .builder()
                            .topHosts(summaryUserList)
                            .build();
                    responseDto.setMessage("Host Leaderboard fetched successfully");
                    responseDto.setData(leaderboard); // Assuming you have a Leaderboard class
                    responseDto.setCount(summaryUserList.size());
                    responseDto.setError(false);

                    return responseDto;
                });
    }

    @Override
    public Mono<LeaderBoardResponseDto> getAgencyLeaderBoard(LeaderboardRequestDto requestDto) {
        if (requestDto.getLimit() == null || requestDto.getLimit() == 0) {
            requestDto.setLimit(10);  // Set a default limit if not provided
        }

        return giftSummaryUseCase.getAgencyGiftSummariesByDate(requestDto.getCreatedAfter(), requestDto.getCreatedBefore(), requestDto.getLimit())
                .flatMap(userBeanSummaries -> {
                    // Map userId to total beans
                    Map<String, Double> agencyIdToBeansMap = userBeanSummaries.stream()
                            .collect(Collectors.toMap(UserBeanSummary::getId, UserBeanSummary::getTotalBeans));

                    // Extract user IDs from the userBeanSummaries list
                    List<String> agencyIdList = userBeanSummaries.stream()
                            .map(UserBeanSummary::getId)
                            .toList();

                    // Fetch user details based on the extracted userIdList
                    return agencyService.getAgenciesByIds(agencyIdList)
                            .doOnError(throwable -> log.error("Error while fetching agencies by ids: {}", throwable.getMessage()))
                            .map(stringUserMap -> {
                                // Create a list of GiftSummaryUser objects
                                List<GiftSummaryUser> summaryUserList = new ArrayList<>();

                                agencyIdList.forEach(agencyId -> {
                                    GiftSummaryUser giftSummaryUser = new GiftSummaryUser();

                                    // Fetch user details from the stringUserMap
                                    AgencyEntity agency = stringUserMap.get(agencyId);

                                    if (agency != null) {
                                        giftSummaryUser.setAgencyId(agencyId);
                                        giftSummaryUser.setDisplayName(agency.getAgencyName());
                                        giftSummaryUser.setProfileImageUrl(agency.getProfileImageUrl());
//                                        giftSummaryUser.setUserLevel(agency.getUserLevel());
                                        giftSummaryUser.setBeansReceived(agencyIdToBeansMap.get(agencyId));  // Set the total beans
                                    }

                                    summaryUserList.add(giftSummaryUser);
                                });

                                // Return the list of GiftSummaryUsers
                                return summaryUserList;
                            });
                })
                .map(summaryUserList -> {
                    // Create the LeaderBoardResponseDto
                    LeaderBoardResponseDto responseDto = new LeaderBoardResponseDto();

                    Leaderboard leaderboard = Leaderboard
                            .builder()
                            .topAgencies(summaryUserList)
                            .build();
                    responseDto.setMessage("Agency Leaderboard fetched successfully");
                    responseDto.setData(leaderboard); // Assuming you have a Leaderboard class
                    responseDto.setCount(summaryUserList.size());
                    responseDto.setError(false);

                    return responseDto;
                });
    }

}
