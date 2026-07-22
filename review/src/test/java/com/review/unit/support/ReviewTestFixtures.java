package com.review.unit.support;

import com.review.api.dto.CreateReviewRequest;
import com.review.api.dto.ModerateReviewRequest;
import com.review.api.dto.StoreReplyRequest;
import com.review.domain.model.PurchaseEligibility;
import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.review.infrastructure.persistence.entity.PurchaseEligibilityJpaEntity;
import com.review.infrastructure.persistence.entity.ReviewJpaEntity;
import com.review.infrastructure.search.document.ReviewDocument;

import java.time.LocalDateTime;

/**
 * review servisine ÖZEL, self-contained test fixture'ları.
 * Bilinçli olarak servisler arasında paylaşılan ortak bir test modülü
 * kullanılmamıştır; review kendi fixture'larına sahiptir.
 */
public final class ReviewTestFixtures {

    public static final LocalDateTime FIXED_CREATED_AT = LocalDateTime.of(2026, 1, 15, 10, 30, 0);

    private ReviewTestFixtures() {
    }

    public static CreateReviewRequest createReviewRequest(String orderId, String productId,
                                                          Integer rating, String comment) {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setOrderId(orderId);
        request.setProductId(productId);
        request.setRating(rating);
        request.setComment(comment);
        return request;
    }

    public static StoreReplyRequest storeReplyRequest(String replyText) {
        StoreReplyRequest request = new StoreReplyRequest();
        request.setReplyText(replyText);
        return request;
    }

    public static ModerateReviewRequest moderateRequest(String status) {
        ModerateReviewRequest request = new ModerateReviewRequest();
        request.setStatus(status);
        return request;
    }

    public static PurchaseEligibility eligibility(String id, String orderId, String customerId,
                                                  String productId, String status) {
        PurchaseEligibility eligibility = new PurchaseEligibility();
        eligibility.setId(id);
        eligibility.setOrderId(orderId);
        eligibility.setCustomerId(customerId);
        eligibility.setProductId(productId);
        eligibility.setStatus(status);
        return eligibility;
    }

    public static PurchaseEligibilityJpaEntity eligibilityEntity(String orderId, String customerId,
                                                                 String productId, String status) {
        PurchaseEligibilityJpaEntity entity = new PurchaseEligibilityJpaEntity();
        entity.setOrderId(orderId);
        entity.setCustomerId(customerId);
        entity.setProductId(productId);
        entity.setStatus(status);
        return entity;
    }

    public static Review review(String id, String productId, String customerId,
                                int rating, ReviewStatus status) {
        Review review = new Review();
        review.setId(id);
        review.setProductId(productId);
        review.setCustomerId(customerId);
        review.setRating(rating);
        review.setComment("yorum-" + id);
        review.setStatus(status);
        review.setCreatedAt(FIXED_CREATED_AT);
        review.setUpdatedAt(FIXED_CREATED_AT);
        return review;
    }

    public static ReviewJpaEntity reviewEntity(String id, String productId, String customerId,
                                               int rating, ReviewStatus status) {
        ReviewJpaEntity entity = new ReviewJpaEntity();
        entity.setId(id);
        entity.setProductId(productId);
        entity.setCustomerId(customerId);
        entity.setRating(rating);
        entity.setComment("yorum-" + id);
        entity.setStatus(status);
        entity.setCreatedAt(FIXED_CREATED_AT);
        entity.setUpdatedAt(FIXED_CREATED_AT);
        return entity;
    }

    public static ReviewDocument reviewDocument(String id, String productId, String customerId,
                                                int rating, String status) {
        ReviewDocument doc = new ReviewDocument();
        doc.setId(id);
        doc.setProductId(productId);
        doc.setCustomerId(customerId);
        doc.setRating(rating);
        doc.setComment("yorum-" + id);
        doc.setStatus(status);
        doc.setCreatedAt(FIXED_CREATED_AT);
        return doc;
    }

    public static OutboxEventJpaEntity outboxEvent(String aggregateId, String type, String payload) {
        OutboxEventJpaEntity event = new OutboxEventJpaEntity();
        event.setAggregateType("Review");
        event.setAggregateId(aggregateId);
        event.setType(type);
        event.setPayload(payload);
        event.setCreatedAt(FIXED_CREATED_AT);
        return event;
    }
}
