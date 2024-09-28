package com.tanvir.features.content.adapter;

import com.tanvir.features.content.adapter.out.persistence.repository.ContentRepository;
import com.tanvir.features.content.application.port.out.ContentPersistencePort;
import com.tanvir.features.content.domain.Content;
import com.tanvir.features.gift.adapter.out.persistence.entity.GiftEntity;
import com.tanvir.features.gift.adapter.out.persistence.repository.GiftRepository;
import com.tanvir.features.gift.application.port.out.GiftPersistencePort;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
@Component
public class ContentPersistenceAdapter implements ContentPersistencePort {
    private final ContentRepository repository;
    private final ModelMapper modelMapper;

    public ContentPersistenceAdapter(ContentRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }


    @Override
    public Mono<Content> getContentById(String contentId) {
        return repository.findById(contentId)
                .map(contentEntity -> modelMapper.map(contentEntity, Content.class));
    }
}
