package com.tanvir.features.gifttransaction.application.service;

import com.google.gson.Gson;
import com.tanvir.core.util.enums.AnnouncementEnum;
import com.tanvir.core.util.enums.Constants;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.commonbusiness.CommonBusiness;
import com.tanvir.features.gift.application.port.in.GiftUseCase;
import com.tanvir.features.gift.domain.valueobjects.ResourceFormat;
import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCase;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.SendGiftRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.SendGiftResponseDto;
import com.tanvir.features.gifttransaction.application.port.out.GiftTransactionPersistencePort;
import com.tanvir.features.gifttransaction.application.port.out.MaxUserPersistencePort;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import com.tanvir.features.gifttransaction.domain.valueobjects.SenderReceiverDto;
import com.tanvir.features.host.application.port.out.HostPersistencePort;
import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.liveroom.application.port.in.LiveRoomUseCase;
import com.tanvir.features.liveroom.application.port.in.dto.response.StreamResponseDto;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.AnnouncementUser;
import com.tanvir.features.liveroom.domain.valueobject.Resource;
import com.tanvir.features.liveroom.domain.valueobject.RoomDataDto;
import com.tanvir.features.user.adapter.out.persistence.mongo.UserMongoRepository;
import com.tanvir.features.user.application.port.in.UserUseCase;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

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

    public GiftTransactionService(GiftTransactionPersistencePort port, UserUseCase userUseCase, Gson gson, HostPersistencePort hostPersistencePort, MaxUserPersistencePort maxUserPersistencePort, UserMongoRepository userRepository, TransactionalOperator transactionalOperator, ModelMapper modelMapper, GiftUseCase giftUseCase, LiveRoomUseCase liveRoomUseCase, LevelUseCase levelUseCase, CachePort cachePort) {
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
    }


    @Override
    public Mono<SendGiftResponseDto> sendGifts(SendGiftRequestDto requestDto) {
        return validateSenderReceiver(requestDto)
                .doOnError(throwable -> log.error("Error while validating sender and receiver"))
                .flatMap(giftTransaction -> this.calculateGiftAmount(giftTransaction, requestDto))
                .doOnError(throwable -> log.error("Error while calculating gift amount"))
                .flatMap(giftTransaction -> this.validateGiftAmount(giftTransaction, requestDto))
                .doOnError(throwable -> log.error("Error while validating gift amount"))
                .map(giftTransaction -> this.buildGiftTransaction(giftTransaction, requestDto))
                .doOnError(throwable -> log.error("Error while building gift transaction"))
                .flatMap(giftTransaction -> port.saveTransaction(giftTransaction)
                        .thenReturn(giftTransaction))
                .doOnError(throwable -> log.error("Error while saving gift transaction"))
                .flatMap(this::announceToFirebaseIfLiveSession)
                .doOnError(throwable -> log.error("Error while announcing gift to firebase"))
                .map(giftTransaction -> this.buildSendGiftResponseDto(giftTransaction, "Gift sent successfully"))
                .as(transactionalOperator::transactional);
    }

    private Mono<GiftTransaction> validateSenderReceiver(SendGiftRequestDto requestDto) {
        return userUseCase.getUserByKeycloakId(requestDto.getKeycloakId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Sender User not found")))
                .flatMap(sender -> userUseCase.getUserById(requestDto.getReceiverId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Receiver User not found")))
                        .map(receiver ->
                                SenderReceiverDto
                                        .builder()
                                        .sender(sender)
                                        .receiver(receiver)
                                        .build()))
                .map(senderReceiverDto -> GiftTransaction
                        .builder()
                        .senderReceiverDto(senderReceiverDto)
                        .build());
    }

    private Mono<GiftTransaction> calculateGiftAmount(GiftTransaction giftTransaction, SendGiftRequestDto requestDto) {
        return giftUseCase.getGiftById(requestDto.getGiftId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Gift not found")))
                .flatMap(gift -> {
                    double giftAmount = gift.getCost() * requestDto.getQuantity();
                    if (giftAmount <= 0) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Invalid gift amount"));
                    }
                    giftTransaction.setBeans(giftAmount);
                    giftTransaction.setGift(gift);
                    return Mono.just(giftTransaction);
                });
    }

    private Mono<GiftTransaction> validateGiftAmount(GiftTransaction giftTransaction, SendGiftRequestDto requestDto) {
        return Mono.just(giftTransaction)
                .filter(giftTransaction1 -> giftTransaction1.getSenderReceiverDto().getSender().getBeans() - giftTransaction1.getBeans() >= 0)
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Insufficient Beans!")));
    }

    private GiftTransaction buildGiftTransaction(GiftTransaction giftTransaction, SendGiftRequestDto requestDto) {
        return GiftTransaction
                .builder()
                .senderId(requestDto.getSenderId())
                .receiverId(requestDto.getReceiverId())
                .giftId(requestDto.getGiftId())
                .quantity(requestDto.getQuantity())
                .beans(giftTransaction.getBeans())
                .liveSession(requestDto.getLiveSession())
                .liveRoomId(requestDto.getLiveRoomId())
                .transactionDate(LocalDate.now())
                .createdOn(LocalDateTime.now())
                .build();
    }

    private Mono<GiftTransaction> announceToFirebaseIfLiveSession(GiftTransaction giftTransaction) {
        return giftTransaction.getLiveSession().equals(Constants.STATUS_YES.getValue())
                ? liveRoomUseCase.getLiveRoomById(giftTransaction.getLiveRoomId())
                    .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "Live Room not found")))
                    .filter(liveRoom -> liveRoom.getStatus().equals(Constants.STATUS_LIVE.getValue()))
                    .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Live Room is not live")))
                    .flatMap(liveRoom -> this.buildGiftAnnouncement(giftTransaction)
                            .flatMap(announcement -> {
                                liveRoom.setAnnouncement(announcement);
                                return cachePort.updateForGift(liveRoom)
                                        .thenReturn(giftTransaction);
                            }))
                : Mono.just(giftTransaction);
    }

    private Mono<Announcement> buildGiftAnnouncement(GiftTransaction giftTransaction) {
        return levelUseCase.getLevelDomainByLevel(giftTransaction.getSenderReceiverDto().getSender().getUserLevel())
                .map(level -> {
                    List<String> imageUrlList = giftTransaction.getGift().getResourceFormats()
                            .stream()
                            .filter(resourceFormat -> resourceFormat.getResourceType().equals("IMAGE"))
                            .map(ResourceFormat::getThumbnailUrl).toList();
                    Announcement announcement = Announcement
                            .builder()
                            .type(AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT.getValue())
                            .time(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString())
                            .messageTemplate(CommonBusiness.getAnnouncementMessage(AnnouncementEnum.ANNOUNCEMENT_TYPE_GIFT.getValue()))
                            .mentionedUser(AnnouncementUser
                                    .builder()
                                    .userId(giftTransaction.getSenderReceiverDto().getSender().getId())
                                    .name(giftTransaction.getSenderReceiverDto().getSender().getDisplayName())
                                    .levelUrl(level.getLevelBadgeUrl())
                                    .build())
                            .gift(Announcement.Gift
                                    .builder()
                                    .quantity(giftTransaction.getQuantity())
                                    .resource(Announcement.Resource
                                            .builder()
                                            .name(giftTransaction.getGift().getName())
                                            .imageUrl(!imageUrlList.isEmpty() ? imageUrlList.get(0) : null)
                                            .build())
                                    .build())
                            .build();
                    log.info("Gift Announcement Built : {}", announcement);
                    return announcement;
                });
    }

    private SendGiftResponseDto buildSendGiftResponseDto(GiftTransaction giftTransaction, String message) {
        SendGiftResponseDto.DataDto dataDto = new SendGiftResponseDto.DataDto();
        dataDto.setGiftId(giftTransaction.getGiftId());
        dataDto.setRecipientId(giftTransaction.getReceiverId());
        dataDto.setSenderId(giftTransaction.getSenderId());
        dataDto.setQuantity(giftTransaction.getQuantity());
        dataDto.setSentOn(giftTransaction.getCreatedOn().toInstant(ZoneOffset.UTC).toString());

        return SendGiftResponseDto
                .builder()
                .message(message)
                .data(dataDto)
                .count(1)
                .build();
    }


}
