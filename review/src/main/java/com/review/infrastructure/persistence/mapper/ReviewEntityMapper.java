package com.review.infrastructure.persistence.mapper;

import com.review.domain.model.Review;
import com.review.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ReviewEntityMapper {
    public Review toDomain(ReviewJpaEntity entity) {
        if (entity == null) return null;
        Review domain = new Review();
        domain.setId(entity.getId());
        domain.setProductId(entity.getProductId());
        domain.setCustomerId(entity.getCustomerId());
        domain.setRating(entity.getRating());
        domain.setComment(entity.getComment());
        domain.setStatus(entity.getStatus());
        domain.setStoreReplyText(entity.getStoreReplyText());
        domain.setStoreRepliedAt(entity.getStoreRepliedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public ReviewJpaEntity toEntity(Review domain) {
        if (domain == null) return null;
        ReviewJpaEntity entity = new ReviewJpaEntity();
        entity.setId(domain.getId());
        entity.setProductId(domain.getProductId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setRating(domain.getRating());
        entity.setComment(domain.getComment());
        entity.setStatus(domain.getStatus());
        entity.setStoreReplyText(domain.getStoreReplyText());
        entity.setStoreRepliedAt(domain.getStoreRepliedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}