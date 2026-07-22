package com.review.infrastructure.persistence.adapter;

import com.review.application.port.out.ReviewCommandPort;
import com.review.domain.model.Review;
import com.review.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.review.infrastructure.persistence.entity.ReviewJpaEntity;
import com.review.infrastructure.persistence.mapper.ReviewEntityMapper;
import com.review.infrastructure.persistence.repository.OutboxEventRepository;
import com.review.infrastructure.persistence.repository.ReviewRepository;
import com.review.infrastructure.search.adapter.ReviewSearchAdapter;
import org.springframework.stereotype.Component;

@Component
public class ReviewPersistenceAdapter implements ReviewCommandPort {

    private final ReviewRepository repository;
    private final OutboxEventRepository outboxEventRepository;
    private final ReviewEntityMapper mapper;
    private final ReviewSearchAdapter searchAdapter; // Senkron ES güncellemesi için varsayım

    public ReviewPersistenceAdapter(ReviewRepository repository, OutboxEventRepository outboxEventRepository, ReviewEntityMapper mapper, ReviewSearchAdapter searchAdapter) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.mapper = mapper;
        this.searchAdapter = searchAdapter;
    }

    @Override
    public Review save(Review review) {
        ReviewJpaEntity entity = mapper.toEntity(review);
        ReviewJpaEntity saved = repository.save(entity);
        Review savedDomain = mapper.toDomain(saved);

        // Elasticsearch CQRS Read model'e senkron yazım (Infrastructure seviyesinde adapter üzerinden)
        searchAdapter.syncToElasticsearch(savedDomain);

        return savedDomain;
    }

    @Override
    public void saveOutboxEvent(String aggregateId, String type, String payload) {
        OutboxEventJpaEntity event = new OutboxEventJpaEntity();
        event.setAggregateType("Review");
        event.setAggregateId(aggregateId);
        event.setType(type);
        event.setPayload(payload);
        outboxEventRepository.save(event);
    }
}