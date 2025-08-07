package com.tanvir.features.content.application.service;

import com.tanvir.core.util.exception.ExceptionHandlerUtil;
import com.tanvir.features.content.adapter.out.persistence.entity.BagEntity;
import com.tanvir.features.content.application.port.in.ContentUseCase;
import com.tanvir.features.content.application.port.out.ContentPersistencePort;
import com.tanvir.features.content.domain.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Service
@Slf4j
public class ContentService implements ContentUseCase {

    private final ContentPersistencePort port;

    public ContentService(ContentPersistencePort port) {
        this.port = port;
    }


    @Override
    public Mono<Content> getContentById(String contentId) {
        return port.getContentById(contentId)
                .doOnRequest(value -> log.info("Requesting content with id: {}", contentId))
                .doOnNext(content -> log.info("Content found: {}", content))
                .doOnError(throwable -> log.error("Error while getting content with id: {}", throwable.getMessage()));
    }

    @Override
    public Mono<BagEntity> getContentByContentIdAndUserId(String contentId, String userId) {
        return port.getContentByContentIdAndUserId(contentId,userId)
                .switchIfEmpty(Mono.error(new ExceptionHandlerUtil(HttpStatus.NOT_FOUND,"Content not found!")));
    }
}
