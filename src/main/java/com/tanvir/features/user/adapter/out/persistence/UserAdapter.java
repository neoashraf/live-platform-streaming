package com.tanvir.features.user.adapter.out.persistence;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAdapter implements DatabasePort {

    private final UserMongoRepository repository;
    private final ModelMapper modelMapper;
    private final UserRepositoryCustomImpl userRepositoryCustom;

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
            .doOnError(throwable -> log.error("Error while getting user from mongo: {}", throwable.getMessage()))
            .doOnSuccess(user -> log.info("Got user from mongo {}", user));
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
            .doOnError(throwable -> log.error("Error while saving user in mongo : {}", user))
            .doOnSuccess(savedEntity -> log.info("User saved in mongo : {}", savedEntity));
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

    private UserEntity mapDomainToEntity(User user) {
        modelMapper.getConfiguration()
                .setSkipNullEnabled(true)
                .setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper.map(user, UserEntity.class);

    }

}
