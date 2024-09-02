package com.tanvir.features.user.application.service;

import com.tanvir.core.util.enums.ExceptionMessages;
import com.tanvir.core.util.exception.ExceptionHandlerUtil;
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

@Service
@Slf4j
public class UserService implements UserUseCase {
    private final DatabasePort userPort;

    public UserService(DatabasePort userPort) {
        this.userPort = userPort;
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

    private Mono<UserInfoResponseDto> handleUserInfoResponseError(String logMessage, Throwable err) {
        log.error("{}: {}", logMessage, err.getMessage());
        return Mono.empty();
    }

    private Mono<UserResponseDto> handleUserResponseError(String logMessage, Throwable err) {
        log.error("{}: {}", logMessage, err.getMessage());
        return Mono.empty();
    }

}