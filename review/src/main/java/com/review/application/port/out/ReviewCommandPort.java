package com.review.application.port.out;

import com.review.domain.model.Review;

public interface ReviewCommandPort {
    Review save(Review review);
    void saveOutboxEvent(String aggregateId, String type, String payload);
}