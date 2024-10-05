package com.tanvir.features.liveroom.adapter.out.persistence;

import com.tanvir.core.util.DateTimeUtil;
import com.tanvir.features.liveroom.adapter.out.persistence.entity.LiveRoomEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseRepository;
import com.tanvir.features.liveroom.application.port.out.CachePort;
import com.tanvir.features.liveroom.domain.LiveRoom;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import com.tanvir.features.liveroom.domain.valueobject.Viewer;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FirebaseAdapter implements CachePort {
    private final LiveRoomFirebaseRepository firebaseRepository;
    private final ModelMapper modelMapper;
    private final DateTimeUtil dateTimeUtil;

    public FirebaseAdapter(LiveRoomFirebaseRepository firebaseRepository, ModelMapper modelMapper, DateTimeUtil dateTimeUtil) {
        this.firebaseRepository = firebaseRepository;
        this.modelMapper = modelMapper;
        this.dateTimeUtil = dateTimeUtil;
    }

    @Override
    public Mono<LiveRoomFirebaseEntity> create(LiveRoomFirebaseEntity entity) {
        return firebaseRepository.createLiveRoom(entity, entity.getId());
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
    public Mono<LiveRoom> update(LiveRoom liveRoom) {

        return firebaseRepository.read(liveRoom.getId())
                .doOnRequest(l -> log.info("Requesting firebase entity with id: {}", liveRoom.getId()))
                .doOnNext(firebaseEntity -> log.debug("Firebase entity received with id: {}", firebaseEntity))
                .map(firebaseEntity -> {
                    List<Viewer> currentViewersInFirebase = new ArrayList<>(firebaseEntity.getViewers() != null ? firebaseEntity.getViewers() : new ArrayList<>());
                    currentViewersInFirebase.add(liveRoom.getViewer());

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
                    List<Viewer> currentViewersInFirebase = new ArrayList<>(firebaseEntity.getViewers() != null ? firebaseEntity.getViewers() : new ArrayList<>());
                    currentViewersInFirebase.remove(liveRoom.getViewer());

                    firebaseEntity.setViewers(currentViewersInFirebase);

                    /*List<Announcement> currentAnnouncementsInFirebase = new ArrayList<>(firebaseEntity.getAnnouncements() != null ? firebaseEntity.getAnnouncements() : new ArrayList<>());
                    if (currentAnnouncementsInFirebase.size() >= 10) {
                        currentAnnouncementsInFirebase = currentAnnouncementsInFirebase.subList(currentAnnouncementsInFirebase.size() - 9, currentAnnouncementsInFirebase.size());
                    }
                    currentAnnouncementsInFirebase.add(liveRoom.getAnnouncement());
                    firebaseEntity.setAnnouncements(currentAnnouncementsInFirebase);*/

                    List<String> currentViewersIdsInFirebase = new ArrayList<>(firebaseEntity.getViewerIds() != null && !firebaseEntity.getViewerIds().isEmpty()
                            ? firebaseEntity.getViewerIds() : new ArrayList<>());
                    currentViewersIdsInFirebase.remove(liveRoom.getViewer().getUserId());

                    firebaseEntity.setViewerIds(currentViewersIdsInFirebase);
                    firebaseEntity.setViewerCount(firebaseEntity.getViewerIds().size());
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
                    // remove viewer from viewers list
                    List<Viewer> currentViewersInFirebase = new ArrayList<>(firebaseEntity.getViewers() != null ? firebaseEntity.getViewers() : new ArrayList<>());
                    currentViewersInFirebase.remove(liveRoom.getViewer());
                    firebaseEntity.setViewers(currentViewersInFirebase);

                    // add announcement to announcements list
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

                    // add announcement to announcements list
                    List<Announcement> currentAnnouncementsInFirebase = new ArrayList<>(firebaseEntity.getAnnouncements() != null ? firebaseEntity.getAnnouncements() : new ArrayList<>());
                    if (currentAnnouncementsInFirebase.size() >= 10) {
                        currentAnnouncementsInFirebase = currentAnnouncementsInFirebase.subList(currentAnnouncementsInFirebase.size() - 9, currentAnnouncementsInFirebase.size());
                    }
                    currentAnnouncementsInFirebase.add(liveRoom.getAnnouncement());
                    firebaseEntity.setAnnouncements(currentAnnouncementsInFirebase);

                    firebaseEntity.setDailyStarProgress(liveRoom.getDailyStarProgress());
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
                .map(firebaseReturnedEntity -> liveRoom);    }
}
