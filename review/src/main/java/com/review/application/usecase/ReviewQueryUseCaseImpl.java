package com.review.application.usecase;

import com.review.api.dto.ReviewResponse;
import com.review.application.port.in.ReviewQueryUseCase;
import com.review.application.port.out.ReviewQueryPort;
import com.review.domain.model.Review;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewQueryUseCaseImpl implements ReviewQueryUseCase {

    private final ReviewQueryPort reviewQueryPort;

    public ReviewQueryUseCaseImpl(ReviewQueryPort reviewQueryPort) {
        this.reviewQueryPort = reviewQueryPort;
    }

    @Override
    public List<ReviewResponse> getProductReviews(String productId) {
        return reviewQueryPort.findActiveByProductId(productId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> getMyReviews(String customerId) {
        return reviewQueryPort.findByCustomerId(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> getAllReviews() {
        return reviewQueryPort.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public double getProductAverageRating(String productId) {
        return reviewQueryPort.getAverageRatingForProduct(productId);
    }

    private ReviewResponse toResponse(Review review) {
        ReviewResponse res = new ReviewResponse();
        res.setId(review.getId());
        res.setProductId(review.getProductId());
        res.setCustomerId(review.getCustomerId());
        res.setRating(review.getRating());
        res.setComment(review.getComment());
        res.setStatus(review.getStatus().name());
        res.setStoreReplyText(review.getStoreReplyText());
        res.setStoreRepliedAt(review.getStoreRepliedAt());
        res.setCreatedAt(review.getCreatedAt());
        return res;
    }
}