package com.tanvir.features.user.adapter.out.persistence.mongo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserMongoRepository extends ReactiveMongoRepository<UserEntity, String>, UserRepositoryCustom {
    Flux<UserEntity> findAllByOrderByCreatedOnDesc();
    Mono<UserEntity> getUserEntityByMaxId(String maxId);
    Mono<UserEntity> getUserEntityByKeycloakId(String keycloakId);
    Mono<UserEntity> getUserEntityByKeycloakIdOrEmail(String keycloakId, String email);
}
