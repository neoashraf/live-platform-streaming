package com.tanvir.features.content.application.port.out;

import com.tanvir.features.content.domain.Content;
import reactor.core.publisher.Mono;

public interface ContentPersistencePort {
    Mono<Content> getContentById(String contentId);
}
