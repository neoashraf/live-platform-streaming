package com.tanvir.features.gifttransaction.application.service;

import com.tanvir.core.util.enums.*;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.content.domain.Content;
import com.tanvir.features.gift.application.port.in.GiftUseCase;
import com.tanvir.features.giftsummary.application.port.in.GiftSummaryUseCase;
import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCase;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.SendGiftRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.GiftTransactionResponseDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.SendGiftResponseDto;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePort;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import com.tanvir.features.gifttransaction.domain.valueobjects.SenderReceiverDto;
import com.tanvir.features.host.application.port.out.HostPersistencePort;
import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.level.domain.Level;
import com.tanvir.features.level.domain.valueobjects.ResourceFormat;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.GiftAnnouncementRepository;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.AnnouncementUser;
import com.tanvir.features.liveroom.domain.valueobject.DailyStarProgress;
import com.tanvir.features.liveroom.domain.valueobject.JoinRequests;
import com.tanvir.features.liveroomactivity.LiveRoomActivityService;
import com.tanvir.features.user.application.port.in.UserUseCase;
import com.tanvir.features.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testng.util.Strings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
public class GiftTransactionService implements GiftTransactionUseCase {

    private final GiftTransactionPersistencePort port;
    private final UserUseCase userUseCase;
    private final HostPersistencePort hostPersistencePort;
    private final TransactionalOperator transactionalOperator;
    private final GiftUseCase giftUseCase;
    private final LiveRoomUseCase liveRoomUseCase;
    private final LevelUseCase levelUseCase;
    private final CachePort cachePort;
    private final GiftSummaryUseCase giftSummaryUseCase;
    private final LiveRoomActivityService liveRoomActivityService;
    private final CommonBusiness commonBusiness;
    private final GiftAnnouncementRepository giftAnnouncementRepository;

    public GiftTransactionService(GiftTransactionPersistencePort port, UserUseCase userUseCase,
                                  HostPersistencePort hostPersistencePort,
                                  TransactionalOperator transactionalOperator,
                                  GiftUseCase giftUseCase, LiveRoomUseCase liveRoomUseCase,
                                  LevelUseCase levelUseCase, CachePort cachePort, GiftSummaryUseCase giftSummaryUseCase,
                                  LiveRoomActivityService liveRoomActivityService, CommonBusiness commonBusiness, GiftAnnouncementRepository giftAnnouncementRepository) {
        this.port = port;
        this.userUseCase = userUseCase;
        this.hostPersistencePort = hostPersistencePort;
        this.transactionalOperator = transactionalOperator;
        this.giftUseCase = giftUseCase;
        this.liveRoomUseCase = liveRoomUseCase;
        this.levelUseCase = levelUseCase;
        this.cachePort = cachePort;
        this.giftSummaryUseCase = giftSummaryUseCase;
        this.liveRoomActivityService = liveRoomActivityService;
        this.commonBusiness = commonBusiness;
        this.giftAnnouncementRepository = giftAnnouncementRepository;
    }


    @Override
    public Mono<SendGiftResponseDto> sendGifts(SendGiftRequestDto requestDto) {
        boolean isLiveRoomGift = requestDto.getLiveRoomId() != null && Constants.STATUS_YES.getValue().equals(requestDto.getLiveSession());
        if (isLiveRoomGift) {
            return liveRoomUseCase.getLiveRoomById(requestDto.getLiveRoomId())
                    .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Live Room not found")))
                    .flatMap(liveRoom -> cachePort.getLiveRoomById(liveRoom.getId())
                            .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Live Room not found")))
                            .map(firebaseEntity -> Tuples.of(liveRoom, firebaseEntity)))
                    .flatMap(liveRoomAndFirebase -> {
                        LiveRoom liveRoom = liveRoomAndFirebase.getT1();
                        LiveRoomFirebaseEntity firebaseEntity = liveRoomAndFirebase.getT2();
                        // Validate all receivers are host or co-host
                        List<String> validIds = new ArrayList<>();
                        validIds.add(firebaseEntity.getHost().getUserId()); // host
                        if (firebaseEntity.getJoinRequests() != null) {
                            validIds.addAll(firebaseEntity.getJoinRequests().stream()
                                    .filter(jr -> Constants.STATUS_APPROVED.getValue().equalsIgnoreCase(jr.getStatus()) || Constants.STATUS_STARTED.getValue().equalsIgnoreCase(jr.getStatus()))
                                    .map(JoinRequests::getUserId)
                                    .toList());
                        }

                        log.info("Valid IDs for live room {}: {}", liveRoom.getId(), validIds);
                        for (String receiverId : requestDto.getReceiverIds()) {
                            if (!validIds.contains(receiverId)) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Receiver must be host or co-host in the live room"));
                            }
                        }

                        Map<String, List<String>> hostAndCoHostMap = new HashMap<>();
                        hostAndCoHostMap.put("Host", Collections.singletonList(firebaseEntity.getHost().getUserId()));
                        if (firebaseEntity.getJoinRequests() != null) {
                            List<String> coHostIds = firebaseEntity.getJoinRequests().stream()
                                    .filter(jr -> Constants.STATUS_APPROVED.getValue().equalsIgnoreCase(jr.getStatus()) || Constants.STATUS_STARTED.getValue().equalsIgnoreCase(jr.getStatus()))
                                    .map(JoinRequests::getUserId)
                                    .toList();
                            hostAndCoHostMap.put("Co-Host", coHostIds);
                        }
                        // Validate sender balance upfront before any processing
                        return validateSenderBalanceUpfront(requestDto)
                                .then(processGiftTransactions(requestDto, liveRoom, hostAndCoHostMap)
                                        .flatMapMany(Flux::fromIterable)
                                        .map(giftTransaction -> {
                                            // Ensure liveRoom is set for each transaction
                                            giftTransaction.setLiveRoom(liveRoom);
                                            return giftTransaction;
                                        })
                                        // Note: Don't call updateHostDailyGemsIfNeeded here for live room gifts
                                        // Daily gems are already updated in calculateHostDailyStarProgress
                                        .collectList()
                                        .as(transactionalOperator::transactional))
                                .flatMap(giftTransactions -> {
                                    // Firebase operations outside transaction boundary
                                    return publishFirebaseUpdates(giftTransactions, liveRoom)
                                            .thenReturn(giftTransactions);
                                })
                                .map(giftTransactions -> this.buildSendGiftResponseDto(giftTransactions, "Gift sent successfully"));
                    });
        } else {
            // Offline gift - also validate upfront
            return validateSenderBalanceUpfront(requestDto)
                    .then(processGiftTransactions(requestDto, null, null)
                            .flatMapMany(Flux::fromIterable)
                            .concatMap(this::updateHostDailyGemsIfNeeded)
                            .collectList()
                            .as(transactionalOperator::transactional))
                    .map(giftTransactions -> this.buildSendGiftResponseDto(giftTransactions, "Gift sent successfully"));
        }
    }

    public static List<Announcement.Resources> buildResourceCollectionRide(Content content) {

        return content.getResourceFormats().stream()
                .filter(resourceFormat -> resourceFormat.getResourceType()
                        .equals(ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue())
                        || resourceFormat.getResourceType().equals(
                        ResourceTypeEnum.RESOURCE_TYPE_ANIMATION.getValue()))
                .map(resourceFormat -> Announcement.Resources.builder()
                        .id(resourceFormat.getResourceId())
                        .type(resourceFormat.getResourceType())
                        .url(resourceFormat.getResourceUrl())
                        .name(content.getName())
                        .thumbnailUrl(resourceFormat.getThumbnailUrl())
                        .build())
                .toList();
    }

    private Mono<Void> validateSenderBalanceUpfront(SendGiftRequestDto requestDto) {
        return userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Sender User not found")))
                .flatMap(sender -> giftUseCase.getGiftById(requestDto.getGiftId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Gift not found")))
                        .flatMap(gift -> {
                            int numReceivers = requestDto.getReceiverIds() != null && !requestDto.getReceiverIds().isEmpty()
                                    ? requestDto.getReceiverIds().size()
                                    : 1;
                            double totalAmount = gift.getCost() * requestDto.getQuantity() * numReceivers;
                            if (sender.getBeans() < totalAmount) {
                                ExceptionHandlerUtil exception = new ExceptionHandlerUtil();
                                exception.setCode(HttpStatus.BAD_REQUEST);
                                exception.setMessage("Insufficient Beans!");
                                return Mono.error(exception);
                            }
                            return Mono.just(sender);
                        }))
                .then();
    }

    private Mono<Void> publishFirebaseUpdates(List<GiftTransaction> giftTransactions, LiveRoom liveRoom) {
        // Build all announcements for all receivers
        return Flux.fromIterable(giftTransactions)
                .concatMap(giftTransaction -> buildGiftAnnouncement(giftTransaction)
                        .flatMap(announcement -> pushPublicGiftAnnouncementIfSupported(announcement).thenReturn(announcement)))
                .collectList()
                .flatMap(announcements -> {
                    // Preserve existing gift session updates by applying them to fresh Firebase entity
                    Map<String, Double> hostGiftUpdates = new HashMap<>();
                    Map<String, Double> coHostGiftUpdates = new HashMap<>();

                    // Calculate accumulated gift amounts for audio streams
                    for (GiftTransaction gt : giftTransactions) {
                        if (gt.getLiveRoom() != null && gt.getLiveRoom().getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())
                                && gt.getLiveRoom().getStatus().equals(Constants.STATUS_LIVE.getValue())) {
                            String receiverId = gt.getReceiverId();
                            if (gt.getSenderReceiverDto().getReceiver().getUserType().equalsIgnoreCase(Constants.HOST_TYPE.getValue())
                                    && receiverId.equalsIgnoreCase(gt.getLiveRoom().getUserId())) {
                                hostGiftUpdates.merge(receiverId, gt.getBeans(), Double::sum);
                            } else {
                                coHostGiftUpdates.merge(receiverId, gt.getBeans(), Double::sum);
                            }
                        }
                    }

                    return cachePort.getLiveRoomById(liveRoom.getId())
                            .flatMap(firebaseEntity2 -> {
                                // Update announcements
                                List<Announcement> current = firebaseEntity2.getAnnouncements() != null ? new ArrayList<>(firebaseEntity2.getAnnouncements()) : new ArrayList<>();
                                current.addAll(announcements);
                                firebaseEntity2.setAnnouncements(current);

                                // Apply host gift session updates
                                hostGiftUpdates.forEach((userId, giftAmount) -> {
                                    if (firebaseEntity2.getHost() != null && userId.equals(firebaseEntity2.getHost().getUserId())) {
                                        double currentAmount = firebaseEntity2.getHost().getGiftsReceivedInThisSession();
                                        double newAmount = currentAmount + giftAmount;
                                        firebaseEntity2.getHost().setGiftsReceivedInThisSession(newAmount);
                                        firebaseEntity2.getHost().setGiftsReceivedInThisSessionString(CommonBusiness.convertToShortName(newAmount));
                                    }
                                });

                                // Apply co-host gift session updates
                                if (firebaseEntity2.getJoinRequests() != null) {
                                    firebaseEntity2.getJoinRequests().forEach(joinRequest -> {
                                        if (coHostGiftUpdates.containsKey(joinRequest.getUserId())
                                                && (Constants.STATUS_APPROVED.getValue().equalsIgnoreCase(joinRequest.getStatus())
                                                || Constants.STATUS_STARTED.getValue().equalsIgnoreCase(joinRequest.getStatus()))) {
                                            double giftAmount = coHostGiftUpdates.get(joinRequest.getUserId());
                                            double currentAmount = joinRequest.getGiftsReceivedInThisSession();
                                            double newAmount = currentAmount + giftAmount;
                                            joinRequest.setGiftsReceivedInThisSession(newAmount);
                                            joinRequest.setGiftsReceivedInThisSessionString(CommonBusiness.convertToShortName(newAmount));
                                        }
                                    });
                                }

                                return cachePort.updateByEntity(firebaseEntity2);
                            })
                            .then();
                });
    }

    // Common transaction pipeline for both online and offline gifts
    private Mono<List<GiftTransaction>> processGiftTransactions(SendGiftRequestDto requestDto, LiveRoom liveRoom, Map<String, List<String>> hostAndCoHostMap) {
        return validateSenderReceiver(requestDto)
                .flatMap(giftTransaction -> this.calculateGiftAmount(giftTransaction, requestDto))
                .flatMap(giftTransaction -> this.validateGiftAmount(giftTransaction, requestDto))
                .flatMapMany(giftTransaction -> this.buildGiftTransaction(giftTransaction, requestDto, liveRoom))
                .flatMap(giftTransaction -> port.saveTransaction(giftTransaction)
                        .doOnError(throwable -> log.error("Error while saving gift transaction"))
                        .map(savedTransaction -> {
                            giftTransaction.setId(savedTransaction.getId());
                            giftTransaction.setHostCohostIdsMap(hostAndCoHostMap);
                            return giftTransaction;
                        }))
                .flatMap(this::updateUserForGiftTransaction)
                .flatMap(giftSummaryUseCase::processGiftSummary)
                .doOnNext(giftTransaction -> log.info("processed gift summary"))
                .flatMap(this::calculateHostDailyStarProgress)
                .doOnNext(giftTransaction -> log.info("sender level : {}", giftTransaction.getSenderReceiverDto().getSender().getUserLevel()))
                .flatMap(giftTransaction -> userUseCase
                        .updateUserForGiftTransaction(giftTransaction.getSenderReceiverDto().getSender(), Constants.USER_TYPE_SENDER.getValue())
                        .doOnError(throwable -> log.error("Error while updating sender user"))
                        .flatMap(user -> userUseCase.updateUserForGiftTransaction(giftTransaction.getSenderReceiverDto().getReceiver(), Constants.USER_TYPE_RECEIVER.getValue())
                                .doOnError(throwable -> log.error(
                                        "Error while updating receiver user"))
                                .thenReturn(giftTransaction)))
                .collectList();
    }

    private Mono<GiftTransaction> calculateHostDailyStarProgress(GiftTransaction giftTransaction) {
        // Only process daily star progress for live room gifts (not offline gifts)
        if (giftTransaction.getHostCohostIdsMap() == null || giftTransaction.getHostCohostIdsMap().get("Host") == null ||
            giftTransaction.getLiveSession() == null || !Constants.STATUS_YES.getValue().equals(giftTransaction.getLiveSession())) {
            log.info("Skipping calculateHostDailyStarProgress - not a live room gift or missing data");
            return Mono.just(giftTransaction);
        }

        // Only process daily star progress for host receivers, not co-hosts
        List<String> hostIds = giftTransaction.getHostCohostIdsMap().get("Host");
        if (!hostIds.contains(giftTransaction.getReceiverId())) {
            log.info("Skipping calculateHostDailyStarProgress - receiver {} is not host (host IDs: {})",
                     giftTransaction.getReceiverId(), hostIds);
            return Mono.just(giftTransaction); // Skip if receiver is not the host
        }

        log.info("Processing calculateHostDailyStarProgress for host {} with {} gems",
                 giftTransaction.getReceiverId(), giftTransaction.getBeans());

        return liveRoomActivityService
                .updateDailyReceivedGems(giftTransaction.getReceiverId(), giftTransaction.getBeans())
                .flatMap(liveRoomActivityEntity -> {
                    double currentGems = liveRoomActivityEntity.getDailyReceivedGems();
                    DailyStarProgress starProgress = this.calculateStarProgress(currentGems);
                    giftTransaction.setDailyStarProgress(starProgress);

                    return Strings.isNotNullAndNotEmpty(giftTransaction.getLiveSession()) && giftTransaction.getLiveSession().equals(Constants.STATUS_YES.getValue())
                            ? liveRoomUseCase.getLiveRoomById(giftTransaction.getLiveRoomId())
                            .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Live Room not found")))
                            .flatMap(liveRoom -> {
                                liveRoom.setHostDailyGems(currentGems);
                                giftTransaction.setLiveRoom(liveRoom);
                                return liveRoomUseCase
                                        .updateLiveRoom(liveRoom)
                                        .flatMap(liveRoom1 ->
                                                cachePort.getLiveRoomById(liveRoom1.getId())
                                                        .flatMap(liveRoomFirebaseEntity -> {
                                                            liveRoomFirebaseEntity.getHost().setDailyStarProgress(starProgress);
                                                            return cachePort.updateForCurrentLiveRoomGiftReceived(liveRoomFirebaseEntity, liveRoom1.getId());
                                                        })
                                                        .map(liveRoomEntity -> giftTransaction));
                            })
                            : Mono.just(giftTransaction);
                })
                .doOnError(throwable -> log.error("Error while calculating host daily star progress"));
    }

    private DailyStarProgress calculateStarProgress(double currentGems) {
        int currentStar = 0;
        double nextStarGems = 0;
        double gemsNeededForNextStar = 0;

        if (currentGems >= 2000000) {
            currentStar = 5;
            nextStarGems = 2000000;
            gemsNeededForNextStar = 0;
        } else if (currentGems >= 1000000) {
            currentStar = 4;
            nextStarGems = 2000000;
            gemsNeededForNextStar = 2000000 - currentGems;
        } else if (currentGems >= 200000) {
            currentStar = 3;
            nextStarGems = 1000000;
            gemsNeededForNextStar = 1000000 - currentGems;
        } else if (currentGems >= 50000) {
            currentStar = 2;
            nextStarGems = 200000;
            gemsNeededForNextStar = 200000 - currentGems;
        } else if (currentGems >= 10000) {
            currentStar = 1;
            nextStarGems = 50000;
            gemsNeededForNextStar = 50000 - currentGems;
        } else {
            currentStar = 0;
            nextStarGems = 10000;
            gemsNeededForNextStar = 10000 - currentGems;
        }

        return DailyStarProgress
                .builder()
                .starLevel(currentStar)
                .nextStarLevel(Math.min(currentStar + 1, 5))
                .dailyReceivedGemsValue(currentGems)
                .dailyReceivedGemsName(CommonBusiness.convertToShortName(currentGems))
                .nextLevelGemsValue(nextStarGems)
                .nextLevelGemsName(CommonBusiness.convertToShortName(nextStarGems))
                .trailingByNextLevelGemsValue(gemsNeededForNextStar)
                .trailingByNextLevelGemsName(CommonBusiness.convertToShortName(gemsNeededForNextStar))
                .build();
    }


    // Push to /giftAnnouncements if supported
    private Mono<Void> pushPublicGiftAnnouncementIfSupported(Announcement announcement) {
        return giftAnnouncementRepository.pushGiftAnnouncement(announcement)
                .thenEmpty(Mono.empty());
    }


    // Update host daily gems if receiver is host
    private Mono<GiftTransaction> updateHostDailyGemsIfNeeded(GiftTransaction giftTransaction) {
        if (giftTransaction.getSenderReceiverDto() != null &&
                giftTransaction.getSenderReceiverDto().getReceiver() != null &&
                Constants.HOST_TYPE.getValue().equalsIgnoreCase(giftTransaction.getSenderReceiverDto().getReceiver().getUserType())) {
            log.info("Processing updateHostDailyGemsIfNeeded for host {} with {} gems",
                     giftTransaction.getSenderReceiverDto().getReceiver().getId(), giftTransaction.getBeans());
            return liveRoomActivityService.updateDailyReceivedGems(
                    giftTransaction.getSenderReceiverDto().getReceiver().getId(),
                    giftTransaction.getBeans()
            ).thenReturn(giftTransaction);
        }
        log.info("Skipping updateHostDailyGemsIfNeeded - receiver is not a host");
        return Mono.just(giftTransaction);
    }

    private JoinRequests receiverValidation(List<JoinRequests> joinRequests, String receiverId) {

        return joinRequests.stream()
                .filter(joinRequest -> joinRequest.getUserId().equals(receiverId) &&
                        (joinRequest.getStatus()
                                .equalsIgnoreCase(Constants.STATUS_STARTED.getValue())
                                ||
                                joinRequest.getStatus().equalsIgnoreCase(
                                        Constants.STATUS_APPROVED.getValue())))
                .findFirst()
                .orElse(null);

    }

    private Mono<GiftTransaction> processGiftTransactionForAudioStream(GiftTransaction giftTransaction) {
        if (giftTransaction.getLiveRoom() != null && giftTransaction.getLiveRoom().getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())
                && giftTransaction.getLiveRoom().getStatus().equals(Constants.STATUS_LIVE.getValue())) {

            return cachePort.getLiveRoomById(giftTransaction.getLiveRoomId())
                    .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found by the given id")))
                    .map(liveRoomFirebaseEntity -> {

                        if (giftTransaction.getSenderReceiverDto().getReceiver().getUserType().equalsIgnoreCase(Constants.HOST_TYPE.getValue())
                                && giftTransaction.getReceiverId().equalsIgnoreCase(giftTransaction.getLiveRoom().getUserId())) {

                            double giftsReceivedInThisSessionAsHost = liveRoomFirebaseEntity.getHost().getGiftsReceivedInThisSession();
                            double newGiftsReceivedInThisSessionAsHost = giftsReceivedInThisSessionAsHost + giftTransaction.getBeans();

                            liveRoomFirebaseEntity.getHost().setGiftsReceivedInThisSession(newGiftsReceivedInThisSessionAsHost);
                            liveRoomFirebaseEntity.getHost().setGiftsReceivedInThisSessionString(CommonBusiness.convertToShortName(newGiftsReceivedInThisSessionAsHost));
                        } else {
                            List<JoinRequests> joinRequests = liveRoomFirebaseEntity.getJoinRequests();

                            if (joinRequests == null) {
                                joinRequests = new ArrayList<>();
                            }

                            String receiverId = giftTransaction.getReceiverId();
                            Optional.ofNullable(this.receiverValidation(joinRequests, receiverId))
                                    .ifPresent(joinRequests2 -> {
                                        double newGiftsReceivedInThisSession = joinRequests2.getGiftsReceivedInThisSession() + giftTransaction.getBeans();
                                        joinRequests2.setGiftsReceivedInThisSession(newGiftsReceivedInThisSession);
                                        joinRequests2.setGiftsReceivedInThisSessionString(CommonBusiness.convertToShortName(newGiftsReceivedInThisSession));
                                    });
                        }
                        return liveRoomFirebaseEntity;
                    })
                    .flatMap(liveRoomFirebaseEntity -> cachePort.updateForCurrentLiveRoomGiftReceived(liveRoomFirebaseEntity, giftTransaction.getLiveRoomId()))
                    .thenReturn(giftTransaction);
        }
        return Mono.just(giftTransaction);
    }


    @Override
    public Mono<GiftTransactionResponseDto> getGiftTransactions(GiftTransactionRequestDto requestDto) {
        log.info("get Bean Transactions command: {}", requestDto);
        return userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                .flatMap(user -> {
                    if (requestDto.getTransactionType()
                            .equals(TransactionTypeEnum.TRANSACTION_TYPE_SENT.getValue())) {
                        requestDto.setSenderId(user.getId());
                    } else if (requestDto.getTransactionType().equals(
                            TransactionTypeEnum.TRANSACTION_TYPE_RECEIVED.getValue())) {
                        requestDto.setReceiverId(user.getId());
                    } else {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,
                                "Invalid transaction type"));
                    }

                    return port.getBeanTransactionsCount(requestDto)
                            .flatMap(aLong -> port.getBeanTransactions(requestDto)
                                    .map(giftTransaction -> {
                                        giftTransaction.setBeansPlain(
                                                CommonBusiness.convertToPlainBigDecimal(
                                                        giftTransaction.getBeans()));
                                        return giftTransaction;
                                    })
                                    .collectList()
                                    .map(giftTransactions -> GiftTransactionResponseDto
                                            .builder()
                                            .message("Gift Transactions fetched successfully")
                                            .data(giftTransactions)
                                            .count(aLong.intValue())
                                            .build()));

                });

    }

    private Mono<GiftTransaction> validateSenderReceiver(SendGiftRequestDto requestDto) {
        return userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,
                        "Sender User not found")))
                .flatMap(sender -> requestDto.getReceiverIds() == null || requestDto.getReceiverIds().isEmpty()
                        ? userUseCase.getUserById(requestDto.getReceiverId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,
                                "Receiver User not found")))
                        .map(receiver -> SenderReceiverDto
                                .builder()
                                .sender(sender)
                                .receiver(receiver)
                                .receivers(List.of(receiver))
                                .build())
                        : userUseCase.getUsersByIds(requestDto.getReceiverIds())
                        .flatMap(userIdUserMap -> {
                            List<String> userIds = userIdUserMap.keySet().stream().toList();
                            List<String> receiverIds = requestDto.getReceiverIds();

                            List<String> nonExistingReceivers = receiverIds
                                    .stream()
                                    .filter(receiverId -> !userIds.contains(receiverId))
                                    .toList();

                            if (!nonExistingReceivers.isEmpty()) {
                                return Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,
                                        "Receiver Users not found: " + String.join(", ", nonExistingReceivers)));
                            }

                            return Mono.just(
                                    SenderReceiverDto
                                            .builder()
                                            .sender(sender)
                                            .receiver(null)
                                            .receivers(userIdUserMap.values().stream().toList())
                                            .build());
                        }))
                .map(senderReceiverDto -> {
                    return GiftTransaction
                            .builder()
                            .senderReceiverDto(senderReceiverDto)
                            .build();
                })
                .doOnError(throwable -> log.error("Error while validating sender and receiver"));
    }

    private Mono<GiftTransaction> calculateGiftAmount(GiftTransaction giftTransaction,
                                                      SendGiftRequestDto requestDto) {
        return giftUseCase.getGiftById(requestDto.getGiftId())
                .switchIfEmpty(Mono.error(
                        new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Gift not found")))
                .flatMap(gift -> {
                    int numReceivers = giftTransaction.getSenderReceiverDto().getReceivers().size();
                    double perReceiverAmount = gift.getCost() * requestDto.getQuantity();
                    double totalAmount = perReceiverAmount * numReceivers;
                    if (perReceiverAmount <= 0 || totalAmount <= 0) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,
                                "Invalid gift amount"));
                    }
                    giftTransaction.setBeans(totalAmount); // sender will be charged total
                    giftTransaction.setGift(gift);
                    return Mono.just(giftTransaction);
                })
                .doOnError(throwable -> log.error("Error while calculating gift amount"));
    }

    private Mono<GiftTransaction> validateGiftAmount(GiftTransaction giftTransaction,
                                                     SendGiftRequestDto requestDto) {
        return Mono.just(giftTransaction)
                .filter(giftTransaction1 -> giftTransaction1.getSenderReceiverDto().getSender()
                        .getBeans() - giftTransaction1.getBeans() >= 0)
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,
                        "Insufficient Beans!")))
                .doOnError(throwable -> log.error("Error while validating gift amount"));
    }

    private Flux<GiftTransaction> buildGiftTransaction(GiftTransaction giftTransaction,
                                                       SendGiftRequestDto requestDto, LiveRoom liveRoom) {
        log.info("Building gift transaction for request: {}", requestDto);

        double perReceiverAmount = giftTransaction.getGift().getCost() * requestDto.getQuantity();

        return Flux.fromIterable(giftTransaction.getSenderReceiverDto().getReceivers())
                .flatMap(receiver -> {
                    if (receiver.getUserType().equals(UserTypeEnum.USER_TYPE_HOST.getValue())) {
                        return hostPersistencePort
                                .getHostByUserId(receiver.getId())
                                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,
                                        "Host not found")))
                                .map(host -> {
                                    giftTransaction.setAgencyId(host.getAgencyMaxId());
                                    return giftTransaction;
                                })
                                .map(giftTransaction1 -> GiftTransaction
                                        .builder()
                                        .senderId(giftTransaction.getSenderReceiverDto().getSender()
                                                .getId())
                                        .receiverId(receiver.getId())
                                        .giftId(requestDto.getGiftId())
                                        .quantity(requestDto.getQuantity())
                                        .beans(perReceiverAmount)
                                        .liveRoom(liveRoom)
                                        .liveSession(requestDto.getLiveSession())
                                        .liveRoomId(requestDto.getLiveRoomId() != null ? requestDto.getLiveRoomId() : "")
                                        .transactionDate(ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
                                                .toString())
                                        .createdOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant())
                                        .senderReceiverDto(SenderReceiverDto
                                                .builder()
                                                .sender(giftTransaction.getSenderReceiverDto().getSender())
                                                .receiver(receiver)
                                                .senderUpdatedFields(giftTransaction.getSenderReceiverDto().getSenderUpdatedFields())
                                                .receiverUpdatedFields(giftTransaction.getSenderReceiverDto().getReceiverUpdatedFields())
                                                .build())
                                        .gift(giftTransaction.getGift())
                                        .agencyId(giftTransaction.getAgencyId())
                                        .build())
                                .doOnError(throwable -> log.error("Error while building gift transaction"));
                    }

                    return Mono.just(GiftTransaction
                            .builder()
                            .senderId(giftTransaction.getSenderReceiverDto().getSender().getId())
                            .receiverId(receiver.getId())
                            .giftId(requestDto.getGiftId())
                            .quantity(requestDto.getQuantity())
                            .beans(perReceiverAmount)
                            .liveRoom(liveRoom != null ? liveRoom : LiveRoom.builder().build())
                            .liveSession(requestDto.getLiveSession())
                            .liveRoomId(requestDto.getLiveRoomId() != null ? requestDto.getLiveRoomId() : "")
                            .transactionDate(ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().toString())
                            .createdOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant())
                            .senderReceiverDto(SenderReceiverDto
                                    .builder()
                                    .sender(giftTransaction.getSenderReceiverDto().getSender())
                                    .receiver(receiver)
                                    .senderUpdatedFields(giftTransaction.getSenderReceiverDto().getSenderUpdatedFields())
                                    .receiverUpdatedFields(giftTransaction.getSenderReceiverDto().getReceiverUpdatedFields())
                                    .build())
                            .gift(giftTransaction.getGift())
                            .build());
                });
    }

    private Mono<Announcement> buildGiftAnnouncementAudioLive(GiftTransaction giftTransaction) {
        return levelUseCase
                .getLevelDomainByLevel(giftTransaction.getSenderReceiverDto().getSender().getUserLevel())
                .zipWith(levelUseCase.getLevelDomainByLevel(giftTransaction.getSenderReceiverDto().getReceiver().getUserLevel()))
                .map(level -> {

                    Level senderLevel = level.getT1();
                    Level receiverLevel = level.getT2();

                    List<ResourceFormat> resourceFormats = giftTransaction.getGift().getResourceFormats();

                    ResourceFormat giftImageResource = CommonBusiness
                            .getResourceFormatByResourceType(resourceFormats, ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                    ResourceFormat senderLevelResource = CommonBusiness
                            .getResourceFormatByResourceType(senderLevel.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                    ResourceFormat receiverLevelResource = CommonBusiness
                            .getResourceFormatByResourceType(receiverLevel.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                    return Announcement
                            .builder()
                            .type(AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT.getValue())
                            .time(ZonedDateTime.now(ZoneOffset.UTC).toString())
                            .messageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT_UPDATE.getValue()))
                            .publisher(AnnouncementUser
                                    .builder()
                                    .userId(giftTransaction.getSenderReceiverDto().getSender().getId())
                                    .maxId(giftTransaction.getSenderReceiverDto().getSender().getMaxId())
                                    .levelUrl(senderLevelResource.getResourceUrl())
                                    .name(giftTransaction.getSenderReceiverDto().getSender().getDisplayName())
                                    .build())
                            .receiverUser(AnnouncementUser
                                    .builder()
                                    .userId(giftTransaction.getSenderReceiverDto().getReceiver().getId())
                                    .name(giftTransaction.getSenderReceiverDto().getReceiver().getDisplayName())
                                    .maxId(giftTransaction.getSenderReceiverDto().getReceiver().getMaxId())
                                    .levelUrl(receiverLevelResource.getResourceUrl())
                                    .build())
                            .gift(Announcement.Gift
                                    .builder()
                                    .quantity(giftTransaction.getQuantity())
                                    .resource(Announcement.Resource
                                            .builder()
                                            .name(giftTransaction.getGift().getName())
                                            .imageUrl(giftImageResource.getThumbnailUrl())
                                            .build())
                                    .resources(buildResourceCollection(resourceFormats, giftTransaction))
                                    .build())
                            .build();
                });
    }


    private Mono<Announcement> buildGiftAnnouncement(GiftTransaction giftTransaction) {
        return levelUseCase
                .getLevelDomainByLevel(giftTransaction.getSenderReceiverDto().getSender().getUserLevel())
                .zipWith(levelUseCase.getLevelDomainByLevel(giftTransaction.getSenderReceiverDto().getReceiver().getUserLevel()))
                .map(level -> {

                    Level senderLevel = level.getT1();
                    Level receiverLevel = level.getT2();

                    List<ResourceFormat> resourceFormats = giftTransaction.getGift().getResourceFormats();

                    ResourceFormat giftImageResource = CommonBusiness
                            .getResourceFormatByResourceType(resourceFormats, ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                    ResourceFormat senderLevelResource = CommonBusiness
                            .getResourceFormatByResourceType(senderLevel.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                    ResourceFormat receiverLevelResource = CommonBusiness
                            .getResourceFormatByResourceType(receiverLevel.getResourceFormats(), ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                    return Announcement
                            .builder()
                            .liveRoomId(giftTransaction.getLiveRoom() != null ? giftTransaction.getLiveRoom().getId() : "")
                            .type(AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT.getValue())
                            .time(ZonedDateTime.now(ZoneOffset.UTC).toString())
                            .messageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT_UPDATE.getValue()))
                            .publisher(AnnouncementUser
                                    .builder()
                                    .userId(giftTransaction.getSenderReceiverDto().getSender().getId())
                                    .maxId(giftTransaction.getSenderReceiverDto().getSender().getMaxId())
                                    .levelUrl(senderLevelResource.getResourceUrl())
                                    .name(giftTransaction.getSenderReceiverDto().getSender().getDisplayName())
                                    .build())
                            .receiverUser(AnnouncementUser
                                    .builder()
                                    .userId(giftTransaction.getSenderReceiverDto().getReceiver().getId())
                                    .name(giftTransaction.getSenderReceiverDto().getReceiver().getDisplayName())
                                    .maxId(giftTransaction.getSenderReceiverDto().getReceiver().getMaxId())
                                    .levelUrl(receiverLevelResource.getResourceUrl())
                                    .build())
                            .gift(Announcement.Gift
                                    .builder()
                                    .quantity(giftTransaction.getQuantity())
                                    .resource(Announcement.Resource
                                            .builder()
                                            .name(giftTransaction.getGift().getName())
                                            .imageUrl(giftImageResource.getThumbnailUrl())
                                            .build())
                                    .resources(buildResourceCollection(resourceFormats, giftTransaction))
                                    .build())
                            .build();
                });
    }

    private List<Announcement.Resources> buildResourceCollection(List<ResourceFormat> resourceFormats,
                                                                 GiftTransaction giftTransaction) {

        return resourceFormats.stream()
                .filter(resourceFormat -> resourceFormat.getResourceType()
                        .equals(ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue())
                        || resourceFormat.getResourceType().equals(
                        ResourceTypeEnum.RESOURCE_TYPE_ANIMATION.getValue()))
                .map(resourceFormat -> Announcement.Resources.builder()
                        .id(resourceFormat.getResourceId())
                        .type(resourceFormat.getResourceType())
                        .name(giftTransaction.getGift().getName())
                        .url(resourceFormat.getResourceUrl())
                        .thumbnailUrl(resourceFormat.getThumbnailUrl())
                        .build())
                .toList();
    }

    private SendGiftResponseDto buildSendGiftResponseDto(List<GiftTransaction> giftTransactions, String message) {
        List<SendGiftResponseDto.DataDto> giftData = new ArrayList<>();

        for (GiftTransaction giftTransaction : giftTransactions) {
            SendGiftResponseDto.DataDto dataDto = new SendGiftResponseDto.DataDto();
            dataDto.setGiftId(giftTransaction.getGiftId());
            dataDto.setRecipientId(giftTransaction.getReceiverId());
            dataDto.setSenderId(giftTransaction.getSenderId());
            dataDto.setQuantity(giftTransaction.getQuantity());
            dataDto.setSentOn(giftTransaction.getCreatedOn().toString());
            dataDto.setBeans(giftTransaction.getSenderReceiverDto().getSender().getBeans());
            dataDto.setBeansValue(CommonBusiness.convertToShortName(dataDto.getBeans()));

            dataDto.setSenderLevel(giftTransaction.getSenderReceiverDto().getSender().getUserLevel());
            dataDto.setLevelBadgeUrl(giftTransaction.getSenderReceiverDto().getSender().getLevelBadgeUrl());
            giftData.add(dataDto);
        }


        return SendGiftResponseDto
                .builder()
                .message(message)
                .data(giftData.get(0))
                .giftData(giftData)
                .count(giftData.size())
                .build();
    }


    private Mono<GiftTransaction> updateUserForGiftTransaction(GiftTransaction giftTransaction) {

        User sender = giftTransaction.getSenderReceiverDto().getSender();
        User receiver = giftTransaction.getSenderReceiverDto().getReceiver();
        Map<String, Object> senderUpdatedFields = giftTransaction.getSenderReceiverDto().getSenderUpdatedFields() == null
                ? new HashMap<>()
                : giftTransaction.getSenderReceiverDto().getSenderUpdatedFields();
        Map<String, Object> receiverUpdatedFields = giftTransaction.getSenderReceiverDto().getReceiverUpdatedFields() == null
                ? new HashMap<>()
                : giftTransaction.getSenderReceiverDto().getReceiverUpdatedFields();

        sender.setSender(true);
        sender.setBeansGifted(sender.getBeansGifted() + giftTransaction.getBeans());
        sender.setBeans(sender.getBeans() - giftTransaction.getBeans());
        receiver.setGems(receiver.getGems() + giftTransaction.getBeans());

        if (sender.getId().equals(receiver.getId())) {
            receiverUpdatedFields.put("beans", sender.getBeans() - giftTransaction.getBeans());
        }


        senderUpdatedFields.put("beans", sender.getBeans() - giftTransaction.getBeans());
        receiverUpdatedFields.put("gems", receiver.getGems() + giftTransaction.getBeans());
        senderUpdatedFields.put("beansGifted", sender.getBeansGifted() + giftTransaction.getBeans());


        return levelUseCase.getLevelByExpValue((long) sender.getBeansGifted())
                .switchIfEmpty(Mono.just(Level.builder().level(0).build()))
                .flatMap(level -> {
                    sender.setUserLevel(level.getLevel());
                    senderUpdatedFields.put("userLevel", level.getLevel());
                    return commonBusiness.setUserLevelUrl(sender)
                            .map(user -> {
                                senderUpdatedFields.put("levelBadgeUrl", user.getLevelBadgeUrl());
                                return user;
                            });
                })
                .map(userMono -> {
                    giftTransaction.getSenderReceiverDto().setSenderUpdatedFields(senderUpdatedFields);
                    giftTransaction.getSenderReceiverDto().setReceiverUpdatedFields(receiverUpdatedFields);
                    return giftTransaction;
                })
                .doOnError(throwable -> log.error("Error while updating user for gift transaction : {}", throwable.getMessage()));
    }

}
