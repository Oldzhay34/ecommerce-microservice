package com.review.application.port.in;

import com.review.api.dto.CreateReviewRequest;
import com.review.api.dto.StoreReplyRequest;
import com.review.api.dto.ModerateReviewRequest;

public interface ReviewCommandUseCase {
    String createReview(String customerId, CreateReviewRequest request);
    void replyToReview(String reviewId, StoreReplyRequest request);
    void moderateReview(String reviewId, ModerateReviewRequest request);
}