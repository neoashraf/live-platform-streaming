package com.tanvir.features.user.application.port.out;

import com.tanvir.features.user.domain.User;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface DatabasePort {

    Mono<User> getById(String id);
    Flux<User> getAll();
    Flux<User> getFilteredUsers(String role, String country, String gender, String active, String searchKey, String maxId, Pageable pageable);
    Mono<User> save(User user);

    Mono<User> getUserByMaxId(String maxId);

    Mono<User> getUserByKeyCloakId(String keycloakId);

    Mono<User> getUserByKeyCloakIdOrEmail(String keycloakId, String email);

    Flux<User> getUsersByIds(List<String> userIdList);
    Mono<User> updateUserFields(String id, Map<String, Object> updatedFields);

    Mono<User> updateUserForGiftTransaction(User user, String userType);
}
