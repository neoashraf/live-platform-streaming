package com.tanvir.features.user.application.service;

import com.tanvir.core.util.enums.ExceptionMessages;
import com.tanvir.core.util.enums.UserTypeEnum;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.gifttransaction.application.port.out.MaxUserPersistencePort;
import com.tanvir.features.host.application.port.out.HostPersistencePort;
import com.tanvir.features.user.application.port.in.UserUseCase;
import com.tanvir.features.user.application.port.in.dto.request.UserRequestDTO;
import com.tanvir.features.user.application.port.in.dto.response.UserInfoResponseDto;
import com.tanvir.features.user.application.port.in.dto.response.UserResponseDto;
import com.tanvir.features.user.application.port.out.DatabasePort;
import com.tanvir.features.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.testng.util.Strings;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserService implements UserUseCase {
    private final DatabasePort userPort;
    private final MaxUserPersistencePort maxUserPersistencePort;
    private final HostPersistencePort hostPersistencePort;

    public UserService(DatabasePort userPort, MaxUserPersistencePort maxUserPersistencePort, HostPersistencePort hostPersistencePort) {
        this.userPort = userPort;
        this.maxUserPersistencePort = maxUserPersistencePort;
        this.hostPersistencePort = hostPersistencePort;
    }

    @Override
    public Mono<UserInfoResponseDto> getUserInfoById(UserRequestDTO user) {
        return userPort.getById(user.getId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, ExceptionMessages.USER_NOT_FOUND.getValue())))
                .map(user1 -> UserInfoResponseDto
                        .builder()
                        .message("Successfully Fetched User")
                        .data(user1)
                        .build())
                .doOnSuccess(user1 -> log.info("Successfully fetched user from mongo: {}", user1));
//                .onErrorResume(err -> handleUserInfoResponseError("Error while getting user by user id", err));
    }

    @Override
    public Mono<UserInfoResponseDto> updateUser(UserRequestDTO requestDTO) {
        return userPort.getById(requestDTO.getId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, ExceptionMessages.USER_NOT_FOUND.getValue())))
                .flatMap(user -> buildUpdatedUser(requestDTO, user))
                .flatMap(userPort::save)
                .map(user -> UserInfoResponseDto
                        .builder()
                        .message("User Updated Successfully")
                        .data(user)
                        .build())
                .doOnSuccess(userMono -> log.info("User after update: {}", userMono));
    }

    private Mono<User> buildUpdatedUser(UserRequestDTO requestDTO, User user) {
        user.setFirstName(getUpdatedValue(requestDTO.getFirstName(), user.getFirstName()));
        user.setLastName(getUpdatedValue(requestDTO.getLastName(), user.getLastName()));
        user.setDisplayName(getUpdatedValue(requestDTO.getDisplayName(), user.getDisplayName()));
        user.setGender(getUpdatedValue(requestDTO.getGender(), user.getGender()));
        user.setDateOfBirth(requestDTO.getDateOfBirth() != null ? requestDTO.getDateOfBirth() : user.getDateOfBirth());
        user.setRoles(getUpdatedRoles(requestDTO.getNewRole(), user.getRoles()));
        user.setUserType(user.getRoles().get(user.getRoles().size() - 1));
        user.setProfileImageId(getUpdatedValue(requestDTO.getProfileImageId(), user.getProfileImageId()));
        user.setProfileImageUrl(getUpdatedValue(requestDTO.getProfileImageUrl(), user.getProfileImageUrl()));
        user.setProfileDescription(requestDTO.getProfileDescription() == null ? user.getProfileDescription() : requestDTO.getProfileDescription());
        user.setUpdatedOn(LocalDateTime.now());
        return Mono.just(user);
    }

    private String getUpdatedValue(String newValue, String currentValue) {
        return Strings.isNotNullAndNotEmpty(newValue) ? newValue : currentValue;
    }

    private List<String> getUpdatedRoles(String newRole, List<String> currentRoles) {
        if (Strings.isNotNullAndNotEmpty(newRole)) {
            currentRoles.add(newRole);
        }
        return currentRoles;
    }





    @Override
    public Mono<User> saveUser(User user) {
        return userPort.save(user)
                .doOnSuccess(savedUser -> log.info("User saved successfully: {}", savedUser));
//                .onErrorMap(throwable -> new ExceptionHandlerUtil(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionMessages.USER_ALREADY_EXISTS.getValue()));
    }

    @Override
    public Mono<User> getUserById(String id) {
        return userPort
                .getById(id)
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, ExceptionMessages.USER_NOT_FOUND.getValue())));
    }

    @Override
    public Mono<User> getUserByMaxId(String maxId) {
        return userPort.getUserByMaxId(maxId)
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, ExceptionMessages.USER_NOT_FOUND.getValue())))
                .doOnRequest(l -> log.info("Request received to get user by max id : {}", maxId))
                .doOnNext(user -> log.info("User fetched by max id: {}", user));
    }

    @Override
    public Mono<User> getUserByKeycloakId(String keycloakId) {
        return userPort.getUserByKeyCloakId(keycloakId)
                .map(user -> {
                    user.setPassword(null);
                    return user;
                })
                .doOnRequest(l -> log.info("Request received to get user by keycloak id : {}", keycloakId));
//                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND, ExceptionMessages.USER_NOT_FOUND.getValue())));
    }



    @Override
    public Mono<User> getUserByKeycloakIdOrEmail(String keycloakId, String email) {
        return userPort.getUserByKeyCloakIdOrEmail(keycloakId, email);
    }

    @Override
    public Mono<User> updateUser(User user) {
        return userPort.getById(user.getId())
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.BAD_REQUEST, ExceptionMessages.USER_NOT_FOUND.getValue())))
                .map(user1 -> {
                    user1.setBeans(user.getBeans());
                    if (user.isSender()) {
                        user1.setBeansGifted(user.getBeansGifted());
                        user1.setUserLevel(user.getUserLevel());
                    } else {
                        user1.setGems(user.getGems());
                    }

                    user1.setUpdatedOn(LocalDateTime.now());
                    return user1;
                })
                .flatMap(userPort::save)
                .doOnRequest(l -> log.info("Request received to update user"))
                .doOnNext(user1 -> log.info("User updated successfully: {}", user))
                .doOnError(throwable -> log.error("Error while updating user profile: {}", throwable.getMessage()))
                .flatMap(user1 ->
                        user.getUserType().equals(UserTypeEnum.USER_TYPE_MAX_USER.getValue())
                                ? maxUserPersistencePort
                                    .getMaxUserEntityByUserId(user.getId())
                                    .flatMap(maxUserEntity -> {
                                        maxUserEntity.setBeans(user.getBeans());
                                        if (user.isSender()) {
                                            maxUserEntity.setBeansGifted(user.getBeansGifted());
                                            maxUserEntity.setUserLevel(user.getUserLevel());
                                        } else {
                                            maxUserEntity.setGems(user.getGems());
                                        }

                                        maxUserEntity.setUpdatedOn(LocalDateTime.now());
                                        return maxUserPersistencePort.saveMaxUserEntity(maxUserEntity);
                                    })
                                    .doOnRequest(l -> log.info("Request received to update max user"))
                                    .doOnSuccess(maxUser -> log.info("Max user updated successfully: {}", maxUser))
                                    .doOnError(throwable -> log.error("Error while updating max user: {}", throwable.getMessage()))
                                    .thenReturn(user)
                                : hostPersistencePort
                                    .getHostByUserId(user.getId())
                                    .flatMap(host -> {
                                        host.setBeans(user.getBeans());
                                        if (user.isSender()) {
                                            host.setBeansGifted(user.getBeansGifted());
                                            host.setUserLevel(user.getUserLevel());
                                        } else {
                                            host.setGems(user.getGems());
                                        }
                                        host.setUpdatedOn(LocalDateTime.now());
                                        return hostPersistencePort.saveHost(host);
                                    })
                                .doOnRequest(l -> log.info("Request received to update host"))
                                .doOnSuccess(host -> log.info("Host updated successfully: {}", host))
                                .doOnError(throwable -> log.error("Error while updating host: {}", throwable.getMessage()))
                                .thenReturn(user)
                );
    }

    @Override
    public Mono<Map<String, User>> getUsersByIds(List<String> userIdList) {
        return userPort.getUsersByIds(userIdList)
                .collectMap(User::getId)
                .doOnRequest(l -> log.info("Request received to get users by ids: {}", userIdList))
                .doOnNext(users -> log.info("Users fetched by ids"));
    }

    private Mono<UserInfoResponseDto> handleUserInfoResponseError(String logMessage, Throwable err) {
        log.error("{}: {}", logMessage, err.getMessage());
        return Mono.empty();
    }

    private Mono<UserResponseDto> handleUserResponseError(String logMessage, Throwable err) {
        log.error("{}: {}", logMessage, err.getMessage());
        return Mono.empty();
    }

    @Override
    public Mono<User> addMoreGems(String maxId, int gemsAmount) {
        return this.getUserByMaxId(maxId)
                .flatMap(user -> {
                    if (user == null) {
                        log.warn("User with maxId {} not found.", maxId);
                        return Mono.error(new RuntimeException("User not found"));
                    }
                    // Update the gems
                    user.setGems(user.getGems() + gemsAmount);
                    log.info("Updating gems for user with maxId {}. New gems count: {}", maxId, user.getGems());
                    return this.updateUser(user); // Save the updated user
                })
                .doOnSuccess(updatedUser -> log.info("Successfully updated user gems for userId: {}", updatedUser.getId()))
                .doOnError(error -> log.error("Error updating gems for user with maxId {}", maxId, error));
    }

}