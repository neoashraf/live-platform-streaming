package com.tanvir.features.content.application.port.out;

import com.tanvir.features.content.adapter.out.persistence.entity.BagEntity;
import com.tanvir.features.content.domain.Content;
import reactor.core.publisher.Mono;

public interface ContentPersistencePort {
    Mono<Content> getContentById(String contentId);
    Mono<BagEntity> getContentByContentIdAndUserId(String contentId, String userId);
}
