package com.tanvir.features.content.adapter.out.persistence.repository;

import com.tanvir.features.content.adapter.out.persistence.entity.ContentEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ContentRepository extends ReactiveMongoRepository<ContentEntity, String> {
}
