package com.tanvir.features.user.adapter.out.persistence;
import com.mongodb.client.result.UpdateResult;
import com.tanvir.core.util.enums.Constants;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.user.adapter.out.persistence.mongo.UserEntity;
import com.tanvir.features.user.adapter.out.persistence.mongo.UserMongoRepository;
import com.tanvir.features.user.adapter.out.persistence.mongo.UserRepositoryCustomImpl;
import com.tanvir.features.user.application.port.out.DatabasePort;
import com.tanvir.features.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.aspectj.lang.reflect.DeclareAnnotation.Kind.Field;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAdapter implements DatabasePort {

    private final UserMongoRepository repository;
    private final ModelMapper modelMapper;
    private final UserRepositoryCustomImpl userRepositoryCustom;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Mono<User> getById(String id) {
        return repository.findById(id)
            .map(entity -> modelMapper.map(entity, User.class))
            /*.map(user -> {
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        user.setUserType(user.getRoles().get(user.getRoles().size() - 1));
                    }
                    return user;
                })*/
            .doOnRequest(value -> log.info("Getting user from mongo wih id {}", id))
            .doOnError(throwable -> log.error("Error while getting user from mongo: {}", throwable.getMessage()));
//            .doOnSuccess(user -> log.info("Got user from mongo {}", user));
    }

    @Override
    public Flux<User> getAll() {
        return repository.findAllByOrderByCreatedOnDesc()
            .map(entity -> modelMapper.map(entity, User.class))
            .map(user -> {
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        user.setUserType(user.getRoles().get(user.getRoles().size() - 1));
                    }
                    return user;
                })
            .doOnRequest(value -> log.info("Getting all users from mongo"))
            .doOnError(throwable -> log.error("Error while getting all users from mongo"))
            .doOnComplete(() -> log.info("Got all users from mongo"));
    }



    @Override
    public Flux<User> getFilteredUsers(String role, String country, String gender, String active, String searchKey, String maxId, Pageable pageable) {
        return userRepositoryCustom
                .findAllByFilters(role, country, gender, active, searchKey, maxId, pageable)
                .map(userEntity -> modelMapper.map(userEntity, User.class))
                .map(user -> {
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        user.setUserType(user.getRoles().get(user.getRoles().size() - 1));
                    }
                    return user;
                })
                .doOnRequest(req -> log.info("Requested to fetch users based on roles: {}", role))
                .doOnComplete(() -> log.info("User found based on role: {}", role))
                .doOnError(err -> log.error("Error while fetching user by roles: {}", err.getMessage()))
                .onErrorResume(err -> {
                    log.error("Error while searching user by roles: {}", err.getMessage());
                    return Flux.empty();
                });

    }

    @Override
    public Mono<User> save(User user) {
        UserEntity entity = modelMapper.map(user, UserEntity.class);
        entity.setCreatedOn(LocalDateTime.now());
        return repository.save(entity)
            .map(savedEntity -> modelMapper.map(savedEntity, User.class))
            .doOnError(throwable -> log.error("Error while saving user in mongo : {}", user));
//            .doOnSuccess(savedEntity -> log.info("User saved in mongo : {}", savedEntity));
    }

    @Override
    public Mono<User> getUserByMaxId(String maxId) {
        return repository.getUserEntityByMaxId(maxId)
                .map(userEntity -> modelMapper.map(userEntity, User.class));
    }

    @Override
    public Mono<User> getUserByKeyCloakId(String keycloakId) {
        return repository.getUserEntityByKeycloakId(keycloakId)
                .map(userEntity -> modelMapper.map(userEntity, User.class));
    }

    @Override
    public Mono<User> getUserByKeyCloakIdOrEmail(String keycloakId, String email) {
        return repository.getUserEntityByKeycloakIdOrEmail(keycloakId, email)
                .map(userEntity -> modelMapper.map(userEntity, User.class));
    }

    @Override
    public Flux<User> getUsersByIds(List<String> userIdList) {
        return repository.findAllByIdIn(userIdList)
                .map(userEntity -> modelMapper.map(userEntity, User.class));
    }

    public Mono<User> updateUserFields(String id, Map<String, Object> updatedFields) {
        // Get valid field names from the entity
        Set<String> validFields = getValidFieldNames(UserEntity.class);

        // Filter out invalid fields
        Map<String, Object> validUpdates = updatedFields.entrySet().stream()
                .filter(entry -> validFields.contains(entry.getKey())) // Only keep valid fields
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // If no valid fields remain, return empty Mono (no update needed)
        if (validUpdates.isEmpty()) {
            return Mono.empty();
        }

        Update update = new Update();
        validUpdates.forEach(update::set);
        update.set("updatedOn", LocalDateTime.now()); // Always update timestamp

        return reactiveMongoTemplate.updateFirst(
                        Query.query(Criteria.where("id").is(id)), update, UserEntity.class)
                .flatMap(updateResult -> repository.findById(id)
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                        .map(entity -> modelMapper.map(entity, User.class)));
    }

    @Override
    public Mono<User> updateUserForGiftTransaction(User user, String userType) {
        Query query = new Query();
        query.addCriteria(Criteria.where("keycloakId").is(user.getKeycloakId()));

        Update update = new Update();

        if (userType.equals(Constants.USER_TYPE_RECEIVER.getValue())) {
            update.set("gems", user.getGems());
        } else {
            update.set("beans", user.getBeans());
            update.set("beansGifted", user.getBeansGifted());
            update.set("userLevel", user.getUserLevel());
            update.set("levelBadgeUrl", user.getLevelBadgeUrl());
        }

        return reactiveMongoTemplate.updateFirst(query, update, UserEntity.class)
                .flatMap(updateResult -> repository.findById(user.getId())
                        .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, "User not found")))
                        .map(entity -> modelMapper.map(entity, User.class)));
    }

    /**
     * Extracts valid field names from an entity class using Reflection.
     */
    private Set<String> getValidFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
    }


    private UserEntity mapDomainToEntity(User user) {
        modelMapper.getConfiguration()
                .setSkipNullEnabled(true)
                .setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper.map(user, UserEntity.class);

    }

}
