package com.tanvir.features.agency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AgencyService {
    private final AgencyRepository agencyRepository;

    public AgencyService(AgencyRepository agencyRepository) {
        this.agencyRepository = agencyRepository;
    }

    public Mono<Map<String, AgencyEntity>> getAgenciesByIds(List<String> agencyIds) {
        return agencyRepository.findAllByMaxIdIn(agencyIds)
                .collectMap(AgencyEntity::getMaxId)
                .doOnRequest(l -> log.info("Request received to get agencies by ids: {}", agencyIds))
                .doOnSuccess(users -> log.info("Users fetched by ids"));
    }
}
