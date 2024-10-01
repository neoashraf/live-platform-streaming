package com.tanvir.features.giftsummary.adapter.out.persistence;

import com.tanvir.features.giftsummary.adapter.out.persistence.repository.GiftSummaryRepository;
import com.tanvir.features.giftsummary.adapter.out.persistence.repository.GiftSummaryRepositoryCustom;
import com.tanvir.features.giftsummary.application.port.out.GiftSummaryPersistencePort;
import com.tanvir.features.giftsummary.domain.GiftSummary;
import com.tanvir.features.leaderboard.domain.UserBeanSummary;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@Slf4j
public class GiftSummaryPersistenceAdapter implements GiftSummaryPersistencePort {

    private final GiftSummaryRepository repository;
    private final GiftSummaryRepositoryCustom customRepository;
    private final ModelMapper modelMapper;

    public GiftSummaryPersistenceAdapter(GiftSummaryRepository repository, GiftSummaryRepositoryCustom customRepository, ModelMapper modelMapper) {
        this.repository = repository;
        this.customRepository = customRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public Mono<List<GiftSummary>> getGiftSummaryByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore) {
        return customRepository.findAllByFilters(userId, null, createdAfter.toInstant(ZoneOffset.UTC), createdBefore.toInstant(ZoneOffset.UTC))
                .map(giftSummaryEntity -> modelMapper.map(giftSummaryEntity, GiftSummary.class))
                .collectList();
    }

    @Override
    public Mono<List<UserBeanSummary>> getHostGiftSummariesByDate(LocalDateTime createdAfter, LocalDateTime createdBefore) {
        return repository.findTopUsersByBeansInDateRange(createdAfter.toInstant(ZoneOffset.UTC), createdBefore.toInstant(ZoneOffset.UTC), 10)
//                .map(giftSummaryEntity -> modelMapper.map(giftSummaryEntity, GiftSummary.class))
                .collectList();
    }

    @Override
    public Mono<Double> getTotalGiftAmountByUserIdAndDate(String userId, LocalDateTime createdAfter, LocalDateTime createdBefore) {
        return customRepository.getTotalBeansByUserIdAndDate(userId, createdAfter.toInstant(ZoneOffset.UTC), createdBefore.toInstant(ZoneOffset.UTC));
    }
}
