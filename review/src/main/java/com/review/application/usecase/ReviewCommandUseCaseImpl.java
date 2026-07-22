package com.review.application.usecase;

import com.review.api.dto.CreateReviewRequest;
import com.review.api.dto.ModerateReviewRequest;
import com.review.api.dto.StoreReplyRequest;
import com.review.application.port.in.ReviewCommandUseCase;
import com.review.application.port.out.PurchaseEligibilityPort;
import com.review.application.port.out.ReviewCommandPort;
import com.review.application.port.out.ReviewQueryPort;
import com.review.domain.exception.DuplicateReviewException;
import com.review.domain.exception.ReviewNotEligibleException;
import com.review.domain.exception.ReviewNotFoundException;
import com.review.domain.model.PurchaseEligibility;
import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ReviewCommandUseCaseImpl implements ReviewCommandUseCase {

    private final ReviewCommandPort commandPort;
    private final ReviewQueryPort queryPort;
    private final PurchaseEligibilityPort eligibilityPort;

    public ReviewCommandUseCaseImpl(ReviewCommandPort commandPort, ReviewQueryPort queryPort, PurchaseEligibilityPort eligibilityPort) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.eligibilityPort = eligibilityPort;
    }

    @Override
    @Transactional
    public String createReview(String customerId, CreateReviewRequest request) {
        PurchaseEligibility eligibility = eligibilityPort.findPendingEligibility(
                request.getOrderId(), customerId, request.getProductId()
        ).orElseThrow(() -> new ReviewNotEligibleException("No pending eligibility found for this order and product."));

        if ("REVIEWED".equals(eligibility.getStatus())) {
            throw new DuplicateReviewException("A review for this product and order already exists.");
        }

        Review review = new Review();
        review.setProductId(request.getProductId());
        review.setCustomerId(customerId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setStatus(ReviewStatus.ACTIVE);

        Review savedReview = commandPort.save(review);
        eligibilityPort.markAsReviewed(eligibility.getId());

        String payload = String.format("{\"productId\":\"%s\",\"rating\":%d}", savedReview.getProductId(), savedReview.getRating());
        commandPort.saveOutboxEvent(savedReview.getId(), "review.created", payload);

        return savedReview.getId();
    }

    @Override
    @Transactional
    public void replyToReview(String reviewId, StoreReplyRequest request) {
        Review review = queryPort.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStoreReplyText(request.getReplyText());
        review.setStoreRepliedAt(LocalDateTime.now());
        commandPort.save(review);
    }

    @Override
    @Transactional
    public void moderateReview(String reviewId, ModerateReviewRequest request) {
        Review review = queryPort.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(ReviewStatus.valueOf(request.getStatus()));
        commandPort.save(review);
    }
}