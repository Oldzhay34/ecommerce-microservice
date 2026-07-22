package com.review.application.port.out;

import com.review.domain.model.PurchaseEligibility;
import java.util.Optional;

public interface PurchaseEligibilityPort {
    Optional<PurchaseEligibility> findPendingEligibility(String orderId, String customerId, String productId);
    void markAsReviewed(String eligibilityId);
    void createIdempotent(String orderId, String customerId, String productId);
}