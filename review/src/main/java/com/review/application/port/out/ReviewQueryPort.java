package com.review.application.port.out;

import com.review.domain.model.Review;
import java.util.List;
import java.util.Optional;

public interface ReviewQueryPort {
    Optional<Review> findById(String id);
    List<Review> findActiveByProductId(String productId);
    List<Review> findByCustomerId(String customerId);
    List<Review> findAll();
    double getAverageRatingForProduct(String productId);
}