package com.review.application.port.in;

import com.review.api.dto.ReviewResponse;
import java.util.List;

public interface ReviewQueryUseCase {
    List<ReviewResponse> getProductReviews(String productId);
    List<ReviewResponse> getMyReviews(String customerId);
    List<ReviewResponse> getAllReviews();
    double getProductAverageRating(String productId);
}