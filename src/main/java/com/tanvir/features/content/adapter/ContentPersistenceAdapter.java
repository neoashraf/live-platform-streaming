package com.tanvir.features.content.adapter;

import com.tanvir.features.content.adapter.out.persistence.entity.BagEntity;
import com.tanvir.features.content.adapter.out.persistence.repository.BagRepository;
import com.tanvir.features.content.adapter.out.persistence.repository.ContentRepository;
import com.tanvir.features.content.application.port.out.ContentPersistencePort;
import com.tanvir.features.content.domain.Content;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
@Component
public class ContentPersistenceAdapter implements ContentPersistencePort {
    private final ContentRepository repository;
    private final ModelMapper modelMapper;
    private final BagRepository bagRepository;

    public ContentPersistenceAdapter(ContentRepository repository, ModelMapper modelMapper, BagRepository bagRepository) {
        this.repository = repository;
        this.modelMapper = modelMapper;
        this.bagRepository = bagRepository;
    }


    @Override
    public Mono<Content> getContentById(String contentId) {
        return repository.findById(contentId)
                .map(contentEntity -> modelMapper.map(contentEntity, Content.class));
    }

    @Override
    public Mono<BagEntity> getContentByContentIdAndUserId(String contentId, String userId) {
        return bagRepository.findByContentIdAndUserId(contentId,userId);
    }
}
