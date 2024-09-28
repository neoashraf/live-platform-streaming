package com.tanvir.features.content.application.port.in;

import com.tanvir.features.content.domain.Content;
import reactor.core.publisher.Mono;

public interface ContentUseCase {
    Mono<Content> getContentById(String contentId);
}
