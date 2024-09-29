package com.tanvir.features.user.application.port.in;

import com.tanvir.features.user.application.port.in.dto.request.UserRequestDTO;
import com.tanvir.features.user.application.port.in.dto.response.UserInfoResponseDto;
import com.tanvir.features.user.application.port.in.dto.response.UserResponseDto;
import com.tanvir.features.user.domain.User;
import reactor.core.publisher.Mono;

public interface UserUseCase {
	Mono<UserInfoResponseDto> getUserInfoById(UserRequestDTO user);
	Mono<UserInfoResponseDto> updateUser(UserRequestDTO requestDTO);

	Mono<User> saveUser(User user);
	Mono<User> getUserById(String id);
	Mono<User> getUserByMaxId(String maxId);
	Mono<User> getUserByKeycloakId(String keycloakId);

	Mono<User> getUserByKeycloakIdOrEmail(String keycloakId, String email);
	Mono<User> updateUser(User user);
}
