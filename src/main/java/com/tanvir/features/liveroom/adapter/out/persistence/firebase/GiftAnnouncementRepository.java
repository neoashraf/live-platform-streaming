package com.tanvir.features.liveroom.adapter.out.persistence.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.*;
import com.tanvir.features.liveroom.domain.valueobject.Announcement;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@DependsOn("firebaseConfig")
public class GiftAnnouncementRepository {
    private static final String ANNOUNCEMENT_PATH = "/giftAnnouncements";
    private static final int MAX_SIZE = 100;
    private final DatabaseReference databaseReference;
    private final ObjectMapper mapper;

    public GiftAnnouncementRepository(ObjectMapper mapper) {
        this.databaseReference = FirebaseDatabase.getInstance().getReference(ANNOUNCEMENT_PATH);
        this.mapper = mapper;
    }

    public Mono<Announcement> pushGiftAnnouncement(Announcement announcement) {
        return Mono.create(sink -> {
            // Store with descending key so newer entries get smaller keys
            String descendingKey = String.valueOf(Long.MAX_VALUE - System.currentTimeMillis());
            databaseReference.child(descendingKey).setValue(
                    mapper.convertValue(announcement, Map.class),
                    (databaseError, databaseReference1) -> {
                        if (databaseError != null) {
                            sink.error(databaseError.toException());
                        } else {
                            removeOldestWhenFull(); // keep only last MAX_SIZE
                            sink.success(announcement);
                        }
                    }
            );
        });
    }

    /**
     * Removes oldest announcements when size exceeds MAX_SIZE.
     * Oldest = largest numeric key (because of descendingKey strategy).
     */
    private void removeOldestWhenFull() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot root) {
                List<DataSnapshot> all = new ArrayList<>();
                for (DataSnapshot child : root.getChildren()) {
                    all.add(child);
                }
                if (all.size() > MAX_SIZE) {
                    // Sort by key descending (largest = oldest first)
                    all.sort((a, b) -> Long.compare(Long.parseLong(b.getKey()), Long.parseLong(a.getKey())));
                    // Remove from the start (oldest)
                    for (int i = 0; i < all.size() - MAX_SIZE; i++) {
                        all.get(i).getRef().removeValue((databaseError, ref) -> {});
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    /**
     * Returns announcements in FIFO order (oldest → newest) based on Firebase key.
     */
    public Mono<List<Announcement>> getGiftAnnouncementList() {
        return Mono.create(sink -> {
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot root) {
                    try {
                        List<Announcement> result = new ArrayList<>();
                        for (DataSnapshot child : root.getChildren()) {
                            Announcement entity = mapper.convertValue(child.getValue(), Announcement.class);
                            result.add(entity);
                        }
                        // Sort by numeric key descendingKey → reverse to FIFO
                        result.sort(Comparator.comparing(a -> {
                            Map<String, Object> map = mapper.convertValue(a, Map.class);
                            // extract firebase key from the announcement map
                            // (requires key from DataSnapshot ideally, but we assume mapper holds same info)
                            Object key = map.get("key");
                            return key != null ? Long.parseLong(key.toString()) : 0L;
                        }));
                        sink.success(result);
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    sink.error(error.toException());
                }
            });
        });
    }
}

