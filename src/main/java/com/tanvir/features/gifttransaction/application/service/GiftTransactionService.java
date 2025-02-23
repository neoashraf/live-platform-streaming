package com.tanvir.features.gifttransaction.application.service;

import com.google.cloud.Tuple;
import com.google.gson.Gson;
import com.tanvir.core.util.enums.*;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.content.domain.Content;
import com.tanvir.features.gift.application.port.in.GiftUseCase;
import com.tanvir.features.level.domain.Level;
import com.tanvir.features.level.domain.valueobjects.ResourceFormat;
import com.tanvir.features.giftsummary.application.port.in.GiftSummaryUseCase;
import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCase;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.SendGiftRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.GiftTransactionResponseDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.SendGiftResponseDto;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePort;
import com.tanvir.features.gifttransaction.application.port.out.MaxUserPersistencePort;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import com.tanvir.features.gifttransaction.domain.valueobjects.SenderReceiverDto;
import com.tanvir.features.host.application.port.out.HostPersistencePort;
import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroom.domain.valueobject.*;
import com.tanvir.features.liveroomactivity.LiveRoomActivityService;
import com.tanvir.features.user.adapter.out.persistence.mongo.UserMongoRepository;
import com.tanvir.features.user.application.port.in.UserUseCase;
import com.tanvir.features.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testng.util.Strings;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@Slf4j
public class GiftTransactionService implements GiftTransactionUseCase {

    private final GiftTransactionPersistencePort port;
    private final UserUseCase userUseCase;
    private final Gson gson;
    private final HostPersistencePort hostPersistencePort;
    private final MaxUserPersistencePort maxUserPersistencePort;
    private final UserMongoRepository userRepository;
    private final TransactionalOperator transactionalOperator;
    private final ModelMapper modelMapper;
    private final GiftUseCase giftUseCase;
    private final LiveRoomUseCase liveRoomUseCase;
    private final LevelUseCase levelUseCase;
    private final CachePort cachePort;
    private final GiftSummaryUseCase giftSummaryUseCase;
    private final LiveRoomActivityService liveRoomActivityService;
    private final CommonBusiness commonBusiness;

    public GiftTransactionService(GiftTransactionPersistencePort port, UserUseCase userUseCase, Gson gson,
                                  HostPersistencePort hostPersistencePort, MaxUserPersistencePort maxUserPersistencePort,
                                  UserMongoRepository userRepository, TransactionalOperator transactionalOperator,
                                  ModelMapper modelMapper, GiftUseCase giftUseCase, LiveRoomUseCase liveRoomUseCase,
                                  LevelUseCase levelUseCase, CachePort cachePort, GiftSummaryUseCase giftSummaryUseCase,
                                  LiveRoomActivityService liveRoomActivityService, CommonBusiness commonBusiness) {
        this.port = port;
        this.userUseCase = userUseCase;
        this.gson = gson;
        this.hostPersistencePort = hostPersistencePort;
        this.maxUserPersistencePort = maxUserPersistencePort;
        this.userRepository = userRepository;
        this.transactionalOperator = transactionalOperator;
        this.modelMapper = modelMapper;
        this.giftUseCase = giftUseCase;
        this.liveRoomUseCase = liveRoomUseCase;
        this.levelUseCase = levelUseCase;
        this.cachePort = cachePort;
        this.giftSummaryUseCase = giftSummaryUseCase;
        this.liveRoomActivityService = liveRoomActivityService;
        this.commonBusiness = commonBusiness;
    }

    @Override
    public Mono<SendGiftResponseDto> sendGifts(SendGiftRequestDto requestDto) {
        return validateSenderReceiver(requestDto)
                .flatMap(giftTransaction -> this.calculateGiftAmount(giftTransaction, requestDto))
                .flatMap(giftTransaction -> this.validateGiftAmount(giftTransaction, requestDto))
                .flatMap(giftTransaction -> this.buildGiftTransaction(giftTransaction, requestDto))
                .flatMap(giftTransaction -> port.saveTransaction(giftTransaction)
                        .doOnError(throwable -> log
                                .error("Error while saving gift transaction"))
                        .map(savedTransaction -> {
                            giftTransaction.setId(savedTransaction.getId());
                            return giftTransaction;
                        }))
                .flatMap(this::updateUserForGiftTransaction)
                .flatMap(giftSummaryUseCase::processGiftSummary)
                .doOnNext(giftTransaction -> log.info("processed gift summary"))
                .flatMap(giftTransaction1 -> giftTransaction1.getLiveSession() != null && giftTransaction1.getLiveSession().equals(Constants.STATUS_YES.getValue())
                        ? liveRoomUseCase.getLiveRoomById(giftTransaction1.getLiveRoomId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Live Room not found")))
                        .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Live Room is not live")))
                        .flatMap(liveRoom -> {
                            giftTransaction1.setLiveRoom(liveRoom);
                            if (giftTransaction1.getReceiverId().equals(liveRoom.getUserId())) {
                                return calculateHostDailyStarProgress(giftTransaction1);
                            }
                            return Mono.just(giftTransaction1);
                        })
                        : Mono.just(giftTransaction1))
                .doOnNext(giftTransaction -> log.info("processed host daily star progress"))
                .flatMap(this::processGiftTransactionForAudioStream)
                .doOnError(throwable -> log.error("Error while calculating host daily star progress"))
                .map(giftTransaction -> {
                    if (giftTransaction.getLiveRoom() != null) {
                        if (giftTransaction.getLiveRoom().getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())) {
                            announceToFirebaseForAudioLiveGift(giftTransaction)
                                    .subscribeOn(Schedulers.boundedElastic()).subscribe();
                        } else {
                            announceToFirebaseIfLiveSession(giftTransaction)
                                    .subscribeOn(Schedulers.boundedElastic()).subscribe();
                        }
                    }
                    return giftTransaction;
                })
                .doOnNext(giftTransaction -> log.info("sender level : {}",
                        giftTransaction.getSenderReceiverDto().getSender().getUserLevel()))
                .flatMap(giftTransaction -> userUseCase
                        .updateUser(giftTransaction.getSenderReceiverDto().getSender(),
                                giftTransaction.getSenderReceiverDto()
                                        .getSenderUpdatedFields())
                        .doOnError(throwable -> log.error("Error while updating sender user"))
                        .flatMap(user -> userUseCase.updateUser(
                                        giftTransaction.getSenderReceiverDto().getReceiver(),
                                        giftTransaction.getSenderReceiverDto()
                                                .getReceiverUpdatedFields())
                                .doOnError(throwable -> log.error(
                                        "Error while updating receiver user"))
                                .thenReturn(giftTransaction)))
                .map(giftTransaction -> this.buildSendGiftResponseDto(giftTransaction,
                        "Gift sent successfully"))
                .as(transactionalOperator::transactional);
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

        if (giftTransaction.getLiveRoom() != null
                && giftTransaction.getLiveRoom().getType()
                .equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())
                && giftTransaction.getLiveRoom().getStatus().equals(Constants.STATUS_LIVE.getValue())) {

            return cachePort.getLiveRoomById(giftTransaction.getLiveRoomId())
                    .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,
                            "LiveRoom not found by the given id")))
                    .map(liveRoomFirebaseEntity -> {

                        if (giftTransaction.getSenderReceiverDto().getReceiver().getUserType()
                                .equalsIgnoreCase(Constants.HOST_TYPE.getValue())
                                && giftTransaction.getReceiverId()
                                .equalsIgnoreCase(giftTransaction
                                        .getLiveRoom()
                                        .getUserId())) {

                            double giftsReceivedInThisSessionAsHost = liveRoomFirebaseEntity.getHost().getGiftsReceivedInThisSession();

                            double newGiftsReceivedInThisSessionAsHost =  giftsReceivedInThisSessionAsHost + giftTransaction.getBeans();

                            liveRoomFirebaseEntity.getHost().setGiftsReceivedInThisSession(newGiftsReceivedInThisSessionAsHost);
                            liveRoomFirebaseEntity.getHost().setGiftsReceivedInThisSessionString(CommonBusiness.convertToShortName(newGiftsReceivedInThisSessionAsHost));
                        } else {
                            List<JoinRequests> joinRequests = liveRoomFirebaseEntity.getJoinRequests();

                            String receiverId = giftTransaction.getReceiverId();
                            Optional.ofNullable(this.receiverValidation(joinRequests, receiverId))
                                    .ifPresent(joinRequests2 -> {

                                        double newGiftsReceivedInThisSession =  joinRequests2.getGiftsReceivedInThisSession() + giftTransaction.getBeans();
                                        joinRequests2.setGiftsReceivedInThisSession(newGiftsReceivedInThisSession);
                                        joinRequests2.setGiftsReceivedInThisSessionString(CommonBusiness.convertToShortName(newGiftsReceivedInThisSession));
                                    });
                        }
                        return liveRoomFirebaseEntity;
                    })
                    .flatMap(liveRoomFirebaseEntity -> cachePort
                            .updateForCurrentLiveRoomGiftReceived(liveRoomFirebaseEntity,
                                    giftTransaction.getLiveRoomId()))
                    .thenReturn(giftTransaction);

        }
        return Mono.just(giftTransaction);
    }

    private Mono<GiftTransaction> calculateHostDailyStarProgress(GiftTransaction giftTransaction) {
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
                .flatMap(sender -> userUseCase.getUserById(requestDto.getReceiverId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,
                                "Receiver User not found")))
                        .map(receiver -> SenderReceiverDto
                                .builder()
                                .sender(sender)
                                .receiver(receiver)
                                .build()))
                .map(senderReceiverDto -> GiftTransaction
                        .builder()
                        .senderReceiverDto(senderReceiverDto)
                        .build())
                .doOnError(throwable -> log.error("Error while validating sender and receiver"));
    }

    private Mono<GiftTransaction> calculateGiftAmount(GiftTransaction giftTransaction,
                                                      SendGiftRequestDto requestDto) {
        return giftUseCase.getGiftById(requestDto.getGiftId())
                .switchIfEmpty(Mono.error(
                        new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Gift not found")))
                .flatMap(gift -> {
                    double giftAmount = gift.getCost() * requestDto.getQuantity();
                    if (giftAmount <= 0) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST,
                                "Invalid gift amount"));
                    }
                    giftTransaction.setBeans(giftAmount);
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

    private Mono<GiftTransaction> buildGiftTransaction(GiftTransaction giftTransaction,
                                                       SendGiftRequestDto requestDto) {
        if (giftTransaction.getSenderReceiverDto().getReceiver().getUserType()
                .equals(UserTypeEnum.USER_TYPE_HOST.getValue())) {
            return hostPersistencePort
                    .getHostByUserId(giftTransaction.getSenderReceiverDto().getReceiver().getId())
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
                            .receiverId(requestDto.getReceiverId())
                            .giftId(requestDto.getGiftId())
                            .quantity(requestDto.getQuantity())
                            .beans(giftTransaction.getBeans())
                            .liveSession(requestDto.getLiveSession())
                            .liveRoomId(requestDto.getLiveRoomId())
                            .transactionDate(ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
                                    .toString())
                            .createdOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant())
                            .senderReceiverDto(giftTransaction.getSenderReceiverDto())
                            .gift(giftTransaction.getGift())
                            .agencyId(giftTransaction.getAgencyId())
                            .build())
                    .doOnError(throwable -> log.error("Error while building gift transaction"));
        }

        return Mono.just(GiftTransaction
                .builder()
                .senderId(giftTransaction.getSenderReceiverDto().getSender().getId())
                .receiverId(requestDto.getReceiverId())
                .giftId(requestDto.getGiftId())
                .quantity(requestDto.getQuantity())
                .beans(giftTransaction.getBeans())
                .liveSession(requestDto.getLiveSession())
                .liveRoomId(requestDto.getLiveRoomId())
                .transactionDate(ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().toString())
                .createdOn(ZonedDateTime.now(ZoneOffset.UTC).toInstant())
                .senderReceiverDto(giftTransaction.getSenderReceiverDto())
                .gift(giftTransaction.getGift())
                .build());
    }

    private Mono<GiftTransaction> announceToFirebaseForAudioLiveGift(GiftTransaction giftTransaction) {

        log.info("getLiveSession : {}", giftTransaction.getLiveSession());

        return giftTransaction.getLiveSession() != null
                && giftTransaction.getLiveSession().equals(Constants.STATUS_YES.getValue())
                ? liveRoomUseCase.getLiveRoomById(giftTransaction.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(
                        HttpStatus.NOT_FOUND,
                        "Live Room not found")))
                .filter(liveRoom -> liveRoom.getStatus().equals(
                        Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(
                        HttpStatus.BAD_REQUEST,
                        "Live Room is not live")))
                .flatMap(liveRoom -> this
                        .buildGiftAnnouncementAudioLive(
                                giftTransaction)
                        .flatMap(announcement -> {
                            liveRoom.setAnnouncement(
                                    announcement);
                            return cachePort.updateForGift(
                                            liveRoom)
                                    .thenReturn(giftTransaction);
                        }))
                : Mono.just(giftTransaction);
    }

    private Mono<Announcement> buildGiftAnnouncementAudioLive(GiftTransaction giftTransaction) {
        return levelUseCase
                .getLevelDomainByLevel(
                        giftTransaction.getSenderReceiverDto().getSender().getUserLevel())
                .zipWith(levelUseCase.getLevelDomainByLevel(
                        giftTransaction.getSenderReceiverDto().getReceiver().getUserLevel()))
                .map(level -> {

                    Level senderLevel = level.getT1();
                    Level receiverLevel = level.getT2();

                    List<ResourceFormat> resourceFormats = giftTransaction.getGift()
                            .getResourceFormats();

                    ResourceFormat giftImageResource = CommonBusiness
                            .getResourceFormatByResourceType(resourceFormats,
                                    ResourceTypeEnum.RESOURCE_TYPE_IMAGE
                                            .getValue());

                    ResourceFormat senderLevelResource = CommonBusiness
                            .getResourceFormatByResourceType(
                                    senderLevel.getResourceFormats(),
                                    ResourceTypeEnum.RESOURCE_TYPE_IMAGE
                                            .getValue());
                    ResourceFormat receiverLevelResource = CommonBusiness
                            .getResourceFormatByResourceType(
                                    receiverLevel.getResourceFormats(),
                                    ResourceTypeEnum.RESOURCE_TYPE_IMAGE
                                            .getValue());

                    return Announcement
                            .builder()
                            .type(AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT.getValue())
                            .time(ZonedDateTime.now(ZoneOffset.UTC).toString())
                            .messageTemplate(CommonBusiness.getAnnouncementMessage(
                                    AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT_UPDATE
                                            .getValue()))
                            .mentionedUser(AnnouncementUser
                                    .builder()
                                    .userId(giftTransaction.getSenderReceiverDto()
                                            .getSender().getId())
                                    .name(giftTransaction.getSenderReceiverDto()
                                            .getSender().getDisplayName())
                                    .maxId(giftTransaction.getSenderReceiverDto()
                                            .getSender().getMaxId())
                                    .levelUrl(senderLevelResource.getResourceUrl())
                                    .build())
                            .receiverUser(AnnouncementUser
                                    .builder()
                                    .userId(giftTransaction.getSenderReceiverDto()
                                            .getReceiver().getId())
                                    .name(giftTransaction.getSenderReceiverDto()
                                            .getReceiver().getDisplayName())
                                    .maxId(giftTransaction.getSenderReceiverDto()
                                            .getReceiver().getMaxId())
                                    .levelUrl(receiverLevelResource
                                            .getResourceUrl())
                                    .build())
                            .gift(Announcement.Gift
                                    .builder()
                                    .quantity(giftTransaction.getQuantity())
                                    .resource(Announcement.Resource
                                            .builder()
                                            .name(giftTransaction.getGift()
                                                    .getName())
                                            .imageUrl(giftImageResource
                                                    .getThumbnailUrl())
                                            .build())
                                    .resources(buildResourceCollection(
                                            resourceFormats,
                                            giftTransaction))
                                    .build())
                            .build();
                });
    }

    private Mono<GiftTransaction> announceToFirebaseIfLiveSession(GiftTransaction giftTransaction) {

        log.info("getLiveSession : {}", giftTransaction.getLiveSession());

        return giftTransaction.getLiveSession() != null
                && giftTransaction.getLiveSession().equals(Constants.STATUS_YES.getValue())
                ? liveRoomUseCase.getLiveRoomById(giftTransaction.getLiveRoomId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(
                        HttpStatus.NOT_FOUND,
                        "Live Room not found")))
                .filter(liveRoom -> liveRoom.getStatus().equals(
                        Constants.STATUS_LIVE.getValue()))
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(
                        HttpStatus.BAD_REQUEST,
                        "Live Room is not live")))
                .flatMap(liveRoom -> this
                        .buildGiftAnnouncement(giftTransaction)
                        .flatMap(announcement -> {
                            liveRoom.setDailyStarProgress(
                                    giftTransaction.getDailyStarProgress());
                            liveRoom.setAnnouncement(
                                    announcement);
                            liveRoom.setHostTotalGems(
                                    giftTransaction.getSenderReceiverDto()
                                            .getReceiver()
                                            .getGems());
                            liveRoom.setHostGemsValue(
                                    CommonBusiness.convertToShortName(
                                            giftTransaction.getSenderReceiverDto()
                                                    .getReceiver()
                                                    .getGems()));
                            return cachePort.updateForGift(
                                            liveRoom)
                                    .thenReturn(giftTransaction);
                        }))
                .doOnError(throwable -> log.error(
                        "Error while announcing gift to firebase"))
                : Mono.just(giftTransaction);
    }

    private Mono<Announcement> buildGiftAnnouncement(GiftTransaction giftTransaction) {
        return levelUseCase
                .getLevelDomainByLevel(
                        giftTransaction.getSenderReceiverDto().getSender().getUserLevel())
                .map(level -> {
                    ResourceFormat imageResource = CommonBusiness.getResourceFormatByResourceType(
                            giftTransaction.getGift().getResourceFormats(),
                            ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());
                    ResourceFormat levelResource = CommonBusiness.getResourceFormatByResourceType(
                            level.getResourceFormats(),
                            ResourceTypeEnum.RESOURCE_TYPE_IMAGE.getValue());

                    List<ResourceFormat> resourceFormats = giftTransaction.getGift()
                            .getResourceFormats();

                    Announcement announcement = Announcement
                            .builder()
                            .type(AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT.getValue())
                            .time(ZonedDateTime.now(ZoneOffset.UTC).toString())
                            .messageTemplate(CommonBusiness.getAnnouncementMessage(
                                    AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT
                                            .getValue()))
                            .mentionedUser(AnnouncementUser
                                    .builder()
                                    .userId(giftTransaction.getSenderReceiverDto()
                                            .getSender().getId())
                                    .name(giftTransaction.getSenderReceiverDto()
                                            .getSender().getDisplayName())
                                    .maxId(giftTransaction.getSenderReceiverDto()
                                            .getSender().getMaxId())
                                    .levelUrl(levelResource.getResourceUrl())
                                    .build())
                            .gift(Announcement.Gift
                                    .builder()
                                    .id(giftTransaction.getGift().getId())
                                    .name(giftTransaction.getGift().getName())
                                    .quantity(giftTransaction.getQuantity())
                                    .resource(Announcement.Resource
                                            .builder()
                                            .name(giftTransaction.getGift()
                                                    .getName())
                                            // .imageUrl(!imageUrlList.isEmpty()
                                            // ? imageUrlList.get(0) : null)
                                            .imageUrl(imageResource
                                                    .getThumbnailUrl())
                                            .build())
                                    .resources(buildResourceCollection(
                                            resourceFormats,
                                            giftTransaction))
                                    .build())
                            .build();
                    // log.info("Gift Announcement Built : {}", announcement);
                    return announcement;
                });
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

    private SendGiftResponseDto buildSendGiftResponseDto(GiftTransaction giftTransaction, String message) {
        SendGiftResponseDto.DataDto dataDto = new SendGiftResponseDto.DataDto();
        dataDto.setGiftId(giftTransaction.getGiftId());
        dataDto.setRecipientId(giftTransaction.getReceiverId());
        dataDto.setSenderId(giftTransaction.getSenderId());
        dataDto.setQuantity(giftTransaction.getQuantity());
        dataDto.setSentOn(giftTransaction.getCreatedOn().toString());
        dataDto.setBeans((Double) giftTransaction.getSenderReceiverDto().getSenderUpdatedFields().getOrDefault(
                "beans",
                giftTransaction.getSenderReceiverDto().getSender().getBeans()));
        dataDto.setBeansValue(CommonBusiness.convertToShortName(dataDto.getBeans()));

        dataDto.setSenderLevel(giftTransaction.getSenderReceiverDto().getSender().getUserLevel());
        dataDto.setLevelBadgeUrl(giftTransaction.getSenderReceiverDto().getSender().getLevelBadgeUrl());

        return SendGiftResponseDto
                .builder()
                .message(message)
                .data(dataDto)
                .count(1)
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

        if (sender.getId().equals(receiver.getId())) {
            receiverUpdatedFields.put("beans", sender.getBeans() - giftTransaction.getBeans());
        }


        senderUpdatedFields.put("beans", sender.getBeans() - giftTransaction.getBeans());
        receiverUpdatedFields.put("gems", receiver.getGems() + giftTransaction.getBeans());
        senderUpdatedFields.put("beansGifted", sender.getBeansGifted() + giftTransaction.getBeans());


        return levelUseCase.getAllLevels()
                .flatMap(levels -> {
                    double beansGifted = sender.getBeansGifted();
                    int level = CommonBusiness.calculateLevel(beansGifted, levels);
                    sender.setUserLevel(level);
                    senderUpdatedFields.put("level", level);
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
