package com.tanvir.features.host.adapter.out.persistence.repository;

import com.tanvir.features.host.adapter.out.persistence.entity.HostEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
@Repository
public class HostRepositoryCustomImpl implements HostRepositoryCustom {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<HostEntity> findAllByFilters(
            String country, String gender, String active, String searchKey, String maxId, String agencyMaxId, String hostType, Pageable pageable) {

        Criteria criteria = new Criteria();

        if (country != null && !country.isEmpty()) {
            criteria.and("country").is(country);
        }
        if (gender != null && !gender.isEmpty()) {
            criteria.and("gender").is(gender);
        }
        if (active != null && !active.isEmpty()) {
            criteria.and("active").is(active);
        }
        if (maxId != null && !maxId.isEmpty()) {
            criteria.and("maxId").is(maxId);
        }
        if (hostType != null && !hostType.isEmpty()) {
            criteria.and("hostType").is(hostType);
        }
        if (agencyMaxId != null && !agencyMaxId.isEmpty()) {
            criteria.and("agencyMaxId").is(agencyMaxId);
        }

        if (searchKey != null && !searchKey.isEmpty()) {
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("firstName").regex(searchKey, "i"),
                    Criteria.where("lastName").regex(searchKey, "i"),
                    Criteria.where("displayName").regex(searchKey, "i"),
                    Criteria.where("maxId").is(searchKey)
            );
            criteria.andOperator(searchCriteria);
        }

//        Query query = new Query(criteria).with(pageable).with(Sort.by(Sort.Direction.DESC, "createdOn"));
        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "createdOn"));

        return reactiveMongoTemplate.find(query, HostEntity.class);
    }
}
