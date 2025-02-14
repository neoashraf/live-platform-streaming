package com.tanvir.common.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.*;
import com.tanvir.features.liveroom.adapter.out.persistence.firebase.LiveRoomFirebaseEntity;
import com.tanvir.features.liveroom.application.service.FirebaseTimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@DependsOn("firebaseConfig")
public class ReactiveFirebaseRepository<T extends BaseFirebaseEntity> {

    private final DatabaseReference databaseReference;
    private final Class<T> entityClass;
    private final ObjectMapper mapper;
    private final FirebaseTimeService firebaseTimeService;

    public ReactiveFirebaseRepository(String referencePath, Class<T> entityClass, ObjectMapper mapper, FirebaseTimeService firebaseTimeService) {
        this.databaseReference = FirebaseDatabase.getInstance().getReference(referencePath);
        this.entityClass = entityClass;
        this.mapper = mapper;
        this.firebaseTimeService = firebaseTimeService;
    }

    public Mono<T> create(T entity) {
        return Mono.create(sink -> {
            databaseReference.setValue(mapper.convertValue(entity, Map.class), (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    sink.error(databaseError.toException());
                } else {
                    sink.success(entity);
                }
            });
        });
    }

    public Mono<T> create(T entity, String key) {
        return Mono.create(sink -> {
            /*String key = databaseReference.push().getKey();
            entity.setId(key);*/
            databaseReference.child(key).setValue(mapper.convertValue(entity, Map.class), (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    sink.error(databaseError.toException());
                } else {
                    sink.success(entity);
                }
            });
        });
    }

    public Mono<LiveRoomFirebaseEntity> createLiveRoom(LiveRoomFirebaseEntity entity, String key) {
        return Mono.create(sink -> {
            /*String key = databaseReference.push().getKey();
            entity.setId(key);*/
            databaseReference.child(key).setValue(mapper.convertValue(entity, Map.class), (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    sink.error(databaseError.toException());
                } else {
                    sink.success(entity);
                }
            });
        })
        /*.doOnSuccess(createdEntity -> {
            // Start the timer when the entity is successfully created
            firebaseTimeService.startTimerV2(entity, databaseReference);
        })*/
        .thenReturn(entity);
    }

    public Mono<Void> stopTimer(LiveRoomFirebaseEntity entity) {
        entity.setStatus("Offline");
        firebaseTimeService.stopTimer();

        // Update status in Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Offline");

        return Mono.create(sink -> {
            databaseReference.child(entity.getId()).updateChildren(updates, (error, ref) -> {
                if (error != null) {
                    sink.error(error.toException());
                } else {
                    sink.success();
                }
            });
        });
    }

    public Mono<T> read(String id) {
        return Mono.create(sink -> databaseReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                T entity = dataSnapshot.getValue(entityClass);
                if (entity != null) {
                    entity.setId(dataSnapshot.getKey());
                    sink.success(entity);
                } else {
                    sink.error(new Exception("Entity not found"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                sink.error(databaseError.toException());
            }
        }));
    }

    public Mono<T> readLast() {
        return Mono.create(sink -> databaseReference.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    // Assuming there's only one child since we used limitToLast(1)
                    DataSnapshot lastChild = dataSnapshot.getChildren().iterator().next();
                    T entity = lastChild.getValue(entityClass);
                    if (entity != null) {
                        entity.setId(lastChild.getKey());
                        sink.success(entity);
                    } else {
                        sink.error(new Exception("Entity not found"));
                    }
                } else {
                    sink.error(new Exception("No entities found"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                sink.error(databaseError.toException());
            }
        }));
    }

    public Mono<T> readSingle() {
        return Mono.create(sink -> databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    T entity = dataSnapshot.getValue(entityClass);
                    if (entity != null) {
                        sink.success(entity);
                    } else {
                        sink.success();
                    }
                } catch (Exception e) {
                    log.error("Error converting dataSnapshot to entity", e);
                    sink.error(e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                sink.error(databaseError.toException());
            }
        }));
    }

    public Mono<List<T>> readAll() {
        return Mono.create(sink -> databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<T> entities = new ArrayList<>();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    T entity = childSnapshot.getValue(entityClass);
                    if (entity != null) {
                        entity.setId(childSnapshot.getKey());
                        entities.add(entity);
                    }
                }
                if (!entities.isEmpty()) {
                    sink.success(entities);
                } else {
                    log.error("No entities found");
                    sink.error(new Exception("No entities found"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                sink.error(databaseError.toException());
            }
        }));
    }

    public Mono<T> update(T entity) {
        return Mono.create(sink -> {
            if (entity.getId() == null) {
                sink.error(new IllegalArgumentException("Entity ID cannot be null"));
            } else {
                databaseReference.child(entity.getId()).setValue(mapper.convertValue(entity, Map.class), (databaseError, databaseReference) -> {
                    if (databaseError != null) {
                        sink.error(databaseError.toException());
                    } else {
                        sink.success(entity);
                    }
                });
            }
        });
    }

    public Mono<T> updateSingle(T entityToUpdate) {
        return Mono.create(sink -> {
            // Assuming there's only one child under this reference or you're updating the reference itself
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Check if data exists before attempting to update
                    if (dataSnapshot.exists()) {
                        // Perform the update
                        databaseReference.setValue(entityToUpdate, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    sink.error(databaseError.toException());
                                } else {
                                    sink.success(entityToUpdate);
                                }
                            }
                        });
                    } else {
                        // Handle case where there is no data to update
                        sink.error(new Exception("No data available to update."));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    sink.error(databaseError.toException());
                }
            });
        });
    }

    public Mono<T> update(T entity, String key) {

        System.out.println("\n Key : "+key+"\n\n");
//        System.out.println("Data being sent: " + mapper.convertValue(entity, Map.class).toString()+"\n\n"); // Debugging

        return Mono.create(sink -> {
            databaseReference.child(key).setValue(mapper.convertValue(entity, Map.class), (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    sink.error(databaseError.toException());
                } else {
                    sink.success(entity);
                }
            });

        });
    }


    public Mono<Boolean> deleteSingle() {
        return Mono.create(sink -> {
            // Assuming deletion at the root or a specific known child
            databaseReference.removeValue(new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        // Handle any error that occurs during the deletion process
                        sink.error(databaseError.toException());
                    } else {
                        // Successfully deleted the data
                        sink.success(true);
                    }
                }
            });
        });
    }

    public Mono<Void> delete(String id) {
        return Mono.create(sink -> databaseReference.child(id).removeValue((databaseError, databaseReference) -> {
            if (databaseError != null) {
                sink.error(databaseError.toException());
            } else {
                sink.success();
            }
        }));
    }
}
