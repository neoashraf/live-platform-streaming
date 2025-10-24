package com.tanvir.features.liveroom.adapter.out.persistence;

import com.tanvir.core.util.DateTimeUtil;
import com.tanvir.core.util.FormatUtil;
import com.tanvir.core.util.enums.Constants;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseRepository;
import com.tanvir.features.liveroom.application.port.in.dto.request.JoinCallRequestDto;
import com.tanvir.features.liveroom.application.port.in.dto.request.JoinCallRequestUpdateDto;
import com.tanvir.features.liveroom.application.port.in.dto.request.SeatNumberDto;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroom.domain.Summary;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.JoinRequests;
import com.tanvir.features.liveroom.domain.valueobject.Viewer;
import com.tanvir.features.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.util.Strings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class FirebaseAdapter implements CachePort {
    private final LiveRoomFirebaseRepository firebaseRepository;
    private final ModelMapper modelMapper;
    private final DateTimeUtil dateTimeUtil;

    @Value("${firebase.operation.timeout.ms:10000}")
    private long firebaseOpTimeoutMs;

    public FirebaseAdapter(LiveRoomFirebaseRepository firebaseRepository, ModelMapper modelMapper, DateTimeUtil dateTimeUtil) {
        this.firebaseRepository = firebaseRepository;
        this.modelMapper = modelMapper;
        this.dateTimeUtil = dateTimeUtil;
    }

    @Override
    public Mono<LiveRoomFirebaseEntity> create(LiveRoomFirebaseEntity entity) {
        log.info("Calling Firebase create for LiveRoom entity: {}", entity);
        log.info("Entity ID: {}", entity.getId());
        log.info("AudioSeatNumber: {}", entity.getAudioSeatNumber());
        return firebaseRepository.createLiveRoom(entity, entity.getId())
                .doOnSubscribe(sub -> log.info("Creating LiveRoom in Firebase with ID {}", entity.getId()))
                .timeout(Duration.ofMillis(firebaseOpTimeoutMs))
                .onErrorMap(TimeoutException.class, e -> {
                    log.error("Firebase create timed out after {} ms for LiveRoom ID {}", firebaseOpTimeoutMs, entity.getId());
                    return new ExceptionHandlerUtil(HttpStatus.GATEWAY_TIMEOUT, "Firebase create timed out");
                })
                .doOnSuccess(result -> log.info("Successfully created LiveRoom: {}", result))
                .doOnError(error -> log.error("Error while creating LiveRoom in Firebase", error));
    }

    @Override
    public Mono<String> delete(String id) {
        /*return firebaseRepository.read(id)
                .flatMap(firebaseEntity -> firebaseRepository.stopTimer(firebaseEntity)
                        .then(firebaseRepository.delete(id)))
                .thenReturn(id);*/
        return firebaseRepository.delete(id)
                .thenReturn(id);
    }

    @Override
    public Mono<LiveRoomEntity> update(LiveRoomEntity entity) {
//        entity.getFans().values().forEach(fan -> fan.setEntryTime(null));
        return firebaseRepository.update(modelMapper.map(entity, LiveRoomFirebaseEntity.class), entity.getId())
                .map(firebaseReturnedEntity -> modelMapper.map(firebaseReturnedEntity, LiveRoomEntity.class));
    }

    @Override
    public Mono<LiveRoomEntity> updateForCurrentLiveRoomGiftReceived(LiveRoomFirebaseEntity liveRoomFirebaseEntity, String liveRoomId) {

//        System.out.println(" \nLiveRoomFireBaseEntity join req in firebase adapter : "+liveRoomFirebaseEntity.getJoinRequests()+"\n\n");
//        System.out.println("\n LiveRoom id : "+liveRoomId+"\n\n");
        return firebaseRepository.update(liveRoomFirebaseEntity, liveRoomId)
                .map(liveRoomFirebaseEntity1 -> modelMapper.map(liveRoomFirebaseEntity1, LiveRoomEntity.class))
                .doOnSuccess(updated -> System.out.println("Firebase Update Successful!"))
                .doOnError(error -> System.err.println("Firebase Update Error: " + error.getMessage()));
    }

    @Override
    public Mono<LiveRoomFirebaseEntity> updateByEntity(LiveRoomFirebaseEntity liveRoomFirebaseEntity) {

        return firebaseRepository.update(liveRoomFirebaseEntity)
                .doOnSuccess(l -> log.info("Requesting firebase entity with id: {}", liveRoomFirebaseEntity.getHost().toString()));
    }

    @Override
    public Mono<LiveRoom> update(LiveRoom liveRoom) {

        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Requesting firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.info("Firebase entity received with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    List<Viewer> currentViewersInFirebase = new ArrayList<>(firebaseEntity.getViewers() != null ? firebaseEntity.getViewers() : new ArrayList<>());
                    currentViewersInFirebase.add(liveRoom.getViewer());

                    currentViewersInFirebase.sort(Comparator.comparingInt(Viewer::getUserLevel).reversed());

                    firebaseEntity.setViewers(currentViewersInFirebase);

                    List<Announcement> currentAnnouncementsInFirebase = new ArrayList<>(firebaseEntity.getAnnouncements() != null ? firebaseEntity.getAnnouncements() : new ArrayList<>());
                    if (currentAnnouncementsInFirebase.size() >= 10) {
                        currentAnnouncementsInFirebase = currentAnnouncementsInFirebase.subList(currentAnnouncementsInFirebase.size() - 9, currentAnnouncementsInFirebase.size());
                    }
                    currentAnnouncementsInFirebase.add(liveRoom.getAnnouncement());
                    firebaseEntity.setAnnouncements(currentAnnouncementsInFirebase);

                    List<String> currentViewersIdsInFirebase = new ArrayList<>(firebaseEntity.getViewerIds() != null && !firebaseEntity.getViewerIds().isEmpty()
                            ? firebaseEntity.getViewerIds() : new ArrayList<>());
                    currentViewersIdsInFirebase.add(liveRoom.getViewer().getUserId());
                    firebaseEntity.setViewerIds(currentViewersIdsInFirebase);
                    firebaseEntity.setViewerCount(firebaseEntity.getViewerIds().size());

                    firebaseEntity.setAudioParticipants(liveRoom.getAudioParticipants());

                    return firebaseEntity;

                })
                .doOnNext(firebaseEntity -> log.debug("Firebase entity to be updated: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForViewerLeave(LiveRoom liveRoom) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Requesting firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Firebase entity received with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    log.info("Viewer : {}", liveRoom.getViewer());
                    List<Viewer> currentViewersInFirebase = new ArrayList<>(firebaseEntity.getViewers() != null ? firebaseEntity.getViewers() : new ArrayList<>());
                    currentViewersInFirebase.removeIf(viewer -> viewer.getUserId().equals(liveRoom.getViewer().getUserId()));
                    firebaseEntity.setViewers(currentViewersInFirebase);

                    List<String> currentViewersIdsInFirebase = new ArrayList<>(firebaseEntity.getViewerIds() != null && !firebaseEntity.getViewerIds().isEmpty()
                            ? firebaseEntity.getViewerIds() : new ArrayList<>());
                    currentViewersIdsInFirebase.remove(liveRoom.getViewer().getUserId());

                    firebaseEntity.setViewerIds(currentViewersIdsInFirebase);
                    firebaseEntity.setViewerCount(firebaseEntity.getViewerIds().size());

                    List<Viewer> currentAudioParticipantsInFirebase = new ArrayList<>(firebaseEntity.getAudioParticipants() != null ? firebaseEntity.getAudioParticipants() : new ArrayList<>());
                    currentAudioParticipantsInFirebase.removeIf(viewer -> viewer.getUserId().equals(liveRoom.getViewer().getUserId()));
                    firebaseEntity.setAudioParticipants(currentAudioParticipantsInFirebase);

                    List<JoinRequests> currentJoinRequests = new ArrayList<>(firebaseEntity.getJoinRequests() != null ? firebaseEntity.getJoinRequests() : new ArrayList<>());
                    log.info("Current join requests: {}", currentJoinRequests);
                    if (!currentJoinRequests.isEmpty()) {

                        List<JoinRequests> updatedList = currentJoinRequests.stream()
                                .peek(joinRequest -> {
                                    if (joinRequest.getUserId().equals(liveRoom.getViewer().getUserId())) {

                                        if (joinRequest.getStatus().equals(Constants.STATUS_STARTED.getValue()) && liveRoom.getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())) {
                                            Map<String, SeatNumberDto> seatMap = firebaseEntity.getSeatMap();
                                            SeatNumberDto seat = seatMap.get(joinRequest.getSeatIndex());
                                            if (seat != null) {
                                                seat.setAvailableStatus(true);
                                                seat.setUserId(null);
                                                seat.setJoinReqId(null);
                                            }
//                                            firebaseEntity.getSeatAvailableStatus().set(joinRequest.getSeatIndex(), false);
                                        }
                                        joinRequest.setStatus(Constants.STATUS_CLOSED.getValue());
                                        joinRequest.setSeatIndex(-1);
                                    }
                                })
                                .toList();
                        firebaseEntity.setJoinRequests(updatedList);
                    }
                    return firebaseEntity;

                })
                .doOnNext(firebaseEntity -> log.debug("Firebase entity to be updated: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForViewerKick(LiveRoom liveRoom) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Requesting firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Firebase entity received with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    List<Viewer> currentViewersInFirebase = new ArrayList<>(firebaseEntity.getViewers() != null ? firebaseEntity.getViewers() : new ArrayList<>());
                    currentViewersInFirebase.removeIf(viewer -> viewer.getUserId().equals(liveRoom.getViewer().getUserId()));
                    firebaseEntity.setViewers(currentViewersInFirebase);

                    List<Announcement> currentAnnouncementsInFirebase = new ArrayList<>(firebaseEntity.getAnnouncements() != null ? firebaseEntity.getAnnouncements() : new ArrayList<>());
                    if (currentAnnouncementsInFirebase.size() >= 10) {
                        currentAnnouncementsInFirebase = currentAnnouncementsInFirebase.subList(currentAnnouncementsInFirebase.size() - 9, currentAnnouncementsInFirebase.size());
                    }
                    currentAnnouncementsInFirebase.add(liveRoom.getAnnouncement());
                    firebaseEntity.setAnnouncements(currentAnnouncementsInFirebase);

                    // remove viewer from viewerIds list
                    List<String> currentViewersIdsInFirebase = new ArrayList<>(firebaseEntity.getViewerIds() != null && !firebaseEntity.getViewerIds().isEmpty()
                            ? firebaseEntity.getViewerIds() : new ArrayList<>());
                    currentViewersIdsInFirebase.remove(liveRoom.getViewer().getUserId());
                    firebaseEntity.setViewerIds(currentViewersIdsInFirebase);
                    firebaseEntity.setViewerCount(firebaseEntity.getViewerIds().size());

                    //add kicked user id in kickedOutUserIds[]
                    List<String> kickedOutUserIds = new ArrayList<>(firebaseEntity.getKickedOutUserIds() != null ? firebaseEntity.getKickedOutUserIds() : new ArrayList<>());
                    if (!kickedOutUserIds.contains(liveRoom.getViewer().getUserId())) {
                        kickedOutUserIds.add(liveRoom.getViewer().getUserId());
                    }
                    firebaseEntity.setKickedOutUserIds(kickedOutUserIds);

                    if (firebaseEntity.getJoinRequests() != null && !firebaseEntity.getJoinRequests().isEmpty()) {
                        List<JoinRequests> updatedJoinRequests = firebaseEntity.getJoinRequests().stream()
                                .peek(joinRequests -> {
                                    if (joinRequests.getUserId().equals(liveRoom.getViewer().getUserId())) {

                                        if (joinRequests.getStatus().equals(Constants.STATUS_STARTED.getValue()) && liveRoom.getType().equals(Constants.LIVE_ROOM_TYPE_AUDIO.getValue())) {
                                            Map<String, SeatNumberDto> seatMap = firebaseEntity.getSeatMap();
                                            SeatNumberDto seat = seatMap.get(joinRequests.getSeatIndex());
                                            if (seat != null) {
                                                seat.setAvailableStatus(true);
                                                seat.setUserId(null);
                                                seat.setJoinReqId(null);
                                            }
//                                            firebaseEntity.getSeatAvailableStatus().set(joinRequests.getSeatIndex(), false);
                                        }
                                        joinRequests.setStatus(Constants.STATUS_CLOSED.getValue());
                                        joinRequests.setSeatIndex(-1);
                                    }
                                })
                                .toList();
                        firebaseEntity.setJoinRequests(updatedJoinRequests);
                    }
                    return firebaseEntity;
                })
                .doOnNext(firebaseEntity -> log.debug("Firebase entity to be updated: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForComment(LiveRoom liveRoom) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Requesting firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Firebase entity received with id: {}", firebaseEntity))
                .map(firebaseEntity -> {

                    // add announcement to announcements list
                    List<Announcement> currentAnnouncementsInFirebase = new ArrayList<>(firebaseEntity.getAnnouncements() != null ? firebaseEntity.getAnnouncements() : new ArrayList<>());
                    if (currentAnnouncementsInFirebase.size() >= 10) {
                        currentAnnouncementsInFirebase = currentAnnouncementsInFirebase.subList(currentAnnouncementsInFirebase.size() - 9, currentAnnouncementsInFirebase.size());
                    }
                    currentAnnouncementsInFirebase.add(liveRoom.getAnnouncement());
                    firebaseEntity.setAnnouncements(currentAnnouncementsInFirebase);


                    return firebaseEntity;
                })
                .doOnNext(firebaseEntity -> log.debug("Firebase entity to be updated: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForGift(LiveRoom liveRoom) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Requesting firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Firebase entity received"))
                .map(firebaseEntity -> {

//                    log.info("\n\n Announcement : {}", liveRoom.getAnnouncement());

                    // add announcement to announcements list
                    List<Announcement> currentAnnouncementsInFirebase = new ArrayList<>(
                            firebaseEntity.getAnnouncements() != null
                                    ? firebaseEntity.getAnnouncements()
                                    : new ArrayList<>()
                    );

                    if (currentAnnouncementsInFirebase.size() >= 10) {
                        currentAnnouncementsInFirebase = currentAnnouncementsInFirebase.subList(currentAnnouncementsInFirebase.size() - 9, currentAnnouncementsInFirebase.size());
                    }
                    currentAnnouncementsInFirebase.add(liveRoom.getAnnouncement());
                    firebaseEntity.setAnnouncements(currentAnnouncementsInFirebase);

                    if (liveRoom.getDailyStarProgress() != null)
                        firebaseEntity.getHost().setDailyStarProgress(liveRoom.getDailyStarProgress());
                    if (liveRoom.getDailyStarProgress() != null)
                        firebaseEntity.getHost().setGems(liveRoom.getHostTotalGems());
                    if (liveRoom.getDailyStarProgress() != null)
                        firebaseEntity.getHost().setGemsValue(liveRoom.getHostGemsValue());

                    return firebaseEntity;
                })
//                .doOnNext(firebaseEntity -> log.info("Firebase entity to be updated: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForJoinPermission(LiveRoom liveRoom) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Requesting firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Firebase entity received with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    firebaseEntity.setEnableJoin(liveRoom.getEnableJoin());
                    return firebaseEntity;
                })
                .doOnNext(firebaseEntity -> log.debug("Firebase entity to be updated: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForAutoJoinPermission(LiveRoom liveRoom) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Requesting firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Firebase entity received with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    firebaseEntity.setEnableAutoJoin(liveRoom.getEnableAutoJoin());
                    return firebaseEntity;
                })
                .doOnNext(firebaseEntity -> log.debug("Firebase entity to be updated: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForJoinRequest(LiveRoom liveRoom) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Request received to get  firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Fetch firebase entity with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    List<JoinRequests> currentJoinRequestsInFirebase = Optional.ofNullable(firebaseEntity.getJoinRequests())
                            .orElseGet(ArrayList::new);

                    if (!currentJoinRequestsInFirebase.isEmpty()) {
                        Optional<JoinRequests> userJoinRequestExists = currentJoinRequestsInFirebase.stream()
                                .filter(joinRequests -> joinRequests.getUserId().equals(liveRoom.getJoinRequests().get(0).getUserId()))
                                .findFirst();

                        userJoinRequestExists.ifPresentOrElse(joinRequests -> {
                            joinRequests.setRequestId(liveRoom.getJoinRequests().get(0).getRequestId());
                            joinRequests.setStatus(Constants.STATUS_PENDING.getValue());

                            joinRequests.setCameraOn(liveRoom.getJoinRequests().get(0).getCameraOn());
                            joinRequests.setCameraView(liveRoom.getJoinRequests().get(0).getCameraView());
                            joinRequests.setMicOn(liveRoom.getJoinRequests().get(0).getMicOn());

                            joinRequests.setProfileLevelUrl(liveRoom.getJoinRequests().get(0).getProfileLevelUrl());
                            joinRequests.setProfileImageUrl(liveRoom.getJoinRequests().get(0).getProfileImageUrl());
                            joinRequests.setDisplayName(liveRoom.getJoinRequests().get(0).getDisplayName());
//                            joinRequests.setSeatIndex(-1);
                        }, () -> {
//                            liveRoom.getJoinRequests().get(0).setSeatIndex(-1);
                            currentJoinRequestsInFirebase.addAll(liveRoom.getJoinRequests());
                        });
                    } else {
                        currentJoinRequestsInFirebase.addAll(liveRoom.getJoinRequests());
                    }
                    firebaseEntity.setJoinRequests(currentJoinRequestsInFirebase);
                    return firebaseEntity;
                })
                .doOnNext(firebaseEntity -> log.debug("Updated firebase entity: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForEndStream(LiveRoom liveRoom) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Request received to get  firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Fetch firebase entity with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    firebaseEntity.setStatus(Constants.STATUS_OFFLINE.getValue());

                    Summary endSummary = firebaseEntity.getSummary();
                    endSummary.setEndedOn(Instant.now().toString());
                    endSummary.setEndedBy(liveRoom.getUserId());
                    endSummary.setDurationInSeconds(liveRoom.getDurationInSeconds());
                    endSummary.setDuration(FormatUtil.convertDurationToString(liveRoom.getDurationInSeconds()));
                    endSummary.setViewerCount(liveRoom.getViewerCount());
                    endSummary.setTotalViewerCount(liveRoom.getTotalViewerCount());
                    endSummary.setHostDailyGems(liveRoom.getHostDailyGems());
                    endSummary.setMaxAudioParticipants(liveRoom.getMaxAudioParticipants());
                    endSummary.setGiftReceivedAmount(liveRoom.getGiftReceivedAmount());
                    endSummary.setGiftReceivedAmountString(String.valueOf(liveRoom.getGiftReceivedAmount()));

                    firebaseEntity.setSummary(endSummary);

                    return firebaseEntity;
                })
                .doOnNext(firebaseEntity -> log.debug("Updated firebase entity: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForProcessingJoinCall(LiveRoom liveRoom, JoinCallRequestDto requestDto) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Request received to get  firebase entity for processing join call with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Fetch firebase entity for processing join call  with id: {}", firebaseEntity))
                .flatMap(firebaseEntity -> {
                    Optional<JoinRequests> optionalJoinRequests = firebaseEntity.getJoinRequests().stream()
                            .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                            .findFirst();

                    if (optionalJoinRequests.isEmpty()) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User request not found"));
                    }

                    JoinRequests joinRequests = optionalJoinRequests.get();
                    joinRequests.setStatus(requestDto.getAction());
                    joinRequests.setReason(requestDto.getReason());

                    if (!requestDto.getAction().equals(Constants.STATUS_DECLINE.getValue())) {
                        this.assignSeat(firebaseEntity, joinRequests);
                    }

                    if (joinRequests.getSeatIndex() > firebaseEntity.getSeatMap().size()) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Seat capacity crosses limit"));
                    }

                    return Mono.just(firebaseEntity);
                })

                .doOnNext(firebaseEntity -> log.debug("Updated firebase entity after processing join call: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    private LiveRoomFirebaseEntity assignSeat(LiveRoomFirebaseEntity firebaseEntity, JoinRequests joinRequests) {
        Map<String, SeatNumberDto> seatAvailableStatus = firebaseEntity.getSeatMap();

        int minSeatIndex = Integer.MAX_VALUE;
        SeatNumberDto selectedSeat = null;

        for (Map.Entry<String, SeatNumberDto> entry : seatAvailableStatus.entrySet()) {
            String key = entry.getKey();
            int seatIndex = Integer.parseInt(key.split("_")[1]);
            SeatNumberDto seat = entry.getValue();

            if (seat.isAvailableStatus() && seatIndex < minSeatIndex) {
                minSeatIndex = seatIndex;
                selectedSeat = seat;
            }
        }

        if (selectedSeat != null) {
            selectedSeat.setAvailableStatus(false);
            selectedSeat.setUserId(joinRequests.getUserId());
            selectedSeat.setJoinReqId(joinRequests.getRequestId());
            joinRequests.setSeatIndex(minSeatIndex);
        } else {
            joinRequests.setSeatIndex(seatAvailableStatus.size() + 1);
        }

        return firebaseEntity;
    }

    @Override
    public Mono<LiveRoom> updateForStartingJoinCall(LiveRoom liveRoom, JoinCallRequestDto requestDto) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Request received to get  firebase entity for processing join call with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Fetch firebase entity for processing join call  with id: {}", firebaseEntity))
                .flatMap(firebaseEntity -> {
                    Optional<JoinRequests> optionalJoinRequests = firebaseEntity.getJoinRequests().stream()
                            .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                            .findFirst();

                    if (optionalJoinRequests.isEmpty()) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User request not found"));
                    }
                    JoinRequests joinRequests = optionalJoinRequests.get();
                    joinRequests.setStatus(Constants.STATUS_STARTED.getValue());

                    return Mono.just(firebaseEntity);
                })
                .doOnNext(firebaseEntity -> log.debug("Updated firebase entity after processing join call: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForCancelJoinRequest(LiveRoom liveRoom, JoinCallRequestDto requestDto) {
        System.out.println("\nRoom Id : " + liveRoom.getId() + "\n");

        return firebaseRepository.read(liveRoom.getId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "LiveRoom not found by the given id")))
                .flatMap(firebaseEntity -> {

                    Optional<JoinRequests> optionalJoinRequests = firebaseEntity.getJoinRequests().stream()
                            .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                            .findFirst();

                    if (optionalJoinRequests.isEmpty()) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User request not found"));
                    }
                    JoinRequests joinRequest = optionalJoinRequests.get();
//                    log.info("\nJoin request : {}",joinRequest);

                    if (!joinRequest.getStatus().equals(Constants.STATUS_PENDING.getValue())) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Request isn't in pending status!"));
                    }
                    joinRequest.setStatus(Constants.STATUS_CANCELLED.getValue());

                    return Mono.just(firebaseEntity);
                })
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateForClosingJoinedCall(LiveRoom liveRoom, JoinCallRequestDto requestDto) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Request received to get  firebase entity for processing join call with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Fetch firebase entity for processing join call  with id: {}", firebaseEntity))
                .flatMap(firebaseEntity -> {
                    Optional<JoinRequests> optionalJoinRequests = firebaseEntity.getJoinRequests().stream()
                            .filter(joinRequests -> joinRequests.getRequestId().equals(requestDto.getRequestId()))
                            .findFirst();

                    if (optionalJoinRequests.isEmpty()) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User request not found"));
                    }
                    JoinRequests joinRequest = optionalJoinRequests.get();

                    if (joinRequest.getStatus().equals(Constants.STATUS_STARTED.getValue())) {
                        Map<String, SeatNumberDto> seatMap = firebaseEntity.getSeatMap();

                        String strSeat = "Seat_" + joinRequest.getSeatIndex();

                        SeatNumberDto seat = seatMap.get(strSeat);
                        if (seat != null) {
                            seat.setAvailableStatus(true);
                            seat.setUserId(null);
                            seat.setJoinReqId(null);
                        }
                    }
                    joinRequest.setStatus(Constants.STATUS_CLOSED.getValue());
                    joinRequest.setSeatIndex(-1);

                    return Mono.just(firebaseEntity);
                })
                .doOnNext(firebaseEntity -> log.debug("Updated firebase entity after processing join call: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoom> updateJoinedCall(LiveRoom liveRoom, JoinCallRequestUpdateDto joinCallRequestUpdateDto) {
        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Request received to get  firebase entity for processing join call with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Fetch firebase entity for processing join call  with id: {}", firebaseEntity))
                .flatMap(firebaseEntity -> {
                    Optional<JoinRequests> optionalJoinRequests = firebaseEntity.getJoinRequests().stream()
                            .filter(joinRequests -> joinRequests.getRequestId().equals(joinCallRequestUpdateDto.getRequestId()))
                            .findFirst();

                    if (optionalJoinRequests.isEmpty()) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User request not found"));
                    } else if (!optionalJoinRequests.get().getStatus().equals(Constants.STATUS_STARTED.getValue())
                            && !optionalJoinRequests.get().getStatus().equals(Constants.STATUS_PENDING.getValue())) {
                        return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join request is not Started or Pending"));
                    }

                    optionalJoinRequests.ifPresentOrElse(joinRequests -> {
                                if (Constants.LIVE_ROOM_TYPE_VIDEO_LOWERCASE.getValue().equalsIgnoreCase(joinCallRequestUpdateDto.getMediaType())) {
                                    joinRequests.setCameraOn(Strings.isNotNullAndNotEmpty(joinCallRequestUpdateDto.getCameraOn())
                                            ? joinCallRequestUpdateDto.getCameraOn()
                                            : joinRequests.getCameraOn());
                                    joinRequests.setMicOn(Strings.isNotNullAndNotEmpty(joinCallRequestUpdateDto.getMicOn())
                                            ? joinCallRequestUpdateDto.getMicOn()
                                            : joinRequests.getMicOn());
                                    joinRequests.setCameraView(Strings.isNotNullAndNotEmpty(joinCallRequestUpdateDto.getCameraView())
                                            ? joinCallRequestUpdateDto.getCameraView()
                                            : joinRequests.getCameraView());

                                } else if (Constants.LIVE_ROOM_TYPE_AUDIO_LOWERCASE.getValue().equalsIgnoreCase(joinCallRequestUpdateDto.getMediaType())) {
                                    joinRequests.setMicOn(Strings.isNotNullAndNotEmpty(joinCallRequestUpdateDto.getMicOn())
                                            ? joinCallRequestUpdateDto.getMicOn()
                                            : joinRequests.getMicOn());
                                }
                            },
                            () -> Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User request not found"))
                    );


                    return Mono.just(firebaseEntity);
                })
                .doOnNext(firebaseEntity -> log.debug("Updated firebase entity after processing join call: {}", firebaseEntity))
                .flatMap(firebaseRepository::update)
                .map(firebaseReturnedEntity -> liveRoom);
    }

    @Override
    public Mono<LiveRoomFirebaseEntity> getLiveRoomById(String id) {
        return firebaseRepository.read(id)
                .doOnRequest(l -> log.info("Request received to get  firebase entity for processing join call with id: {}", id))
                .doOnNext(firebaseEntity -> log.debug("Fetch firebase entity for processing join call  with id: {}", firebaseEntity));
    }

    @Override
    public Mono<LiveRoomFirebaseEntity> updateAudioSeatMap(String liveRoomId, Integer seatNumber, SeatNumberDto seatNumberDto) {
        if (Strings.isNullOrEmpty(seatNumberDto.getJoinReqId()))
            return Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, "Join request ID is required for updating seat map"));

        return firebaseRepository.read(liveRoomId)
                .doOnRequest(l -> log.info("Request received to get  firebase entity with id: {}", liveRoomId))
                .doOnNext(firebaseEntity -> log.debug("Fetch firebase entity with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    // Free old seat if any
                    firebaseEntity.getSeatMap().forEach((seatNum, seatDto) -> {
                        if (seatNumberDto.getUserId().equals(seatDto.getUserId())) {
                            seatDto.setAvailableStatus(true);
                            seatDto.setUserId(null);
                            seatDto.setJoinReqId(null);
                        }
                    });

                    // Update the seat map with the new seat number
                    firebaseEntity.getSeatMap().put("Seat_" + seatNumber, seatNumberDto);

                    if (firebaseEntity.getJoinRequests() != null) {
                        firebaseEntity.getJoinRequests().forEach(joinRequests -> {
                            if (joinRequests.getRequestId().equals(seatNumberDto.getJoinReqId())) {
                                joinRequests.setSeatIndex(seatNumber);
                            }
                        });
                    }

                    return firebaseEntity;
                })
                .doOnNext(firebaseEntity -> log.debug("Updated firebase entity: {}", firebaseEntity))
                .flatMap(firebaseRepository::update);
    }
}
