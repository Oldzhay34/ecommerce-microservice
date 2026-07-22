package com.review.infrastructure.search.mapper;

import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.search.document.ReviewDocument;
import org.springframework.stereotype.Component;

@Component
public class ReviewDocumentMapper {
    public ReviewDocument toDocument(Review review) {
        if (review == null) return null;
        ReviewDocument doc = new ReviewDocument();
        doc.setId(review.getId());
        doc.setProductId(review.getProductId());
        doc.setCustomerId(review.getCustomerId());
        doc.setRating(review.getRating());
        doc.setComment(review.getComment());
        doc.setStatus(review.getStatus() != null ? review.getStatus().name() : null);
        doc.setStoreReplyText(review.getStoreReplyText());
        doc.setStoreRepliedAt(review.getStoreRepliedAt());
        doc.setCreatedAt(review.getCreatedAt());
        return doc;
    }

    public Review toDomain(ReviewDocument doc) {
        if (doc == null) return null;
        Review review = new Review();
        review.setId(doc.getId());
        review.setProductId(doc.getProductId());
        review.setCustomerId(doc.getCustomerId());
        review.setRating(doc.getRating());
        review.setComment(doc.getComment());
        if (doc.getStatus() != null) review.setStatus(ReviewStatus.valueOf(doc.getStatus()));
        review.setStoreReplyText(doc.getStoreReplyText());
        review.setStoreRepliedAt(doc.getStoreRepliedAt());
        review.setCreatedAt(doc.getCreatedAt());
        return review;
    }
}