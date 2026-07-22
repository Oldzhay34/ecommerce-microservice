package com.review.api.controller;

import com.review.api.dto.CreateReviewRequest;
import com.review.api.dto.ModerateReviewRequest;
import com.review.api.dto.ReviewResponse;
import com.review.api.dto.StoreReplyRequest;
import com.review.application.port.in.ReviewCommandUseCase;
import com.review.application.port.in.ReviewQueryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewCommandUseCase commandUseCase;
    private final ReviewQueryUseCase queryUseCase;

    public ReviewController(ReviewCommandUseCase commandUseCase, ReviewQueryUseCase queryUseCase) {
        this.commandUseCase = commandUseCase;
        this.queryUseCase = queryUseCase;
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getProductReviews(@PathVariable String productId) {
        List<ReviewResponse> reviews = queryUseCase.getProductReviews(productId);
        double averageRating = queryUseCase.getProductAverageRating(productId);

        Map<String, Object> response = new HashMap<>();
        response.put("averageRating", averageRating);
        response.put("reviews", reviews);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<String> createReview(@Valid @RequestBody CreateReviewRequest request, Authentication authentication) {
        String customerId = authentication.getName(); // JWT subject (userId)
        String reviewId = commandUseCase.createReview(customerId, request);
        return ResponseEntity.ok(reviewId);
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(Authentication authentication) {
        String customerId = authentication.getName();
        return ResponseEntity.ok(queryUseCase.getMyReviews(customerId));
    }

    @PatchMapping("/{id}/reply")
    public ResponseEntity<Void> replyToReview(@PathVariable String id, @Valid @RequestBody StoreReplyRequest request) {
        commandUseCase.replyToReview(id, request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/moderate")
    public ResponseEntity<Void> moderateReview(@PathVariable String id, @Valid @RequestBody ModerateReviewRequest request) {
        commandUseCase.moderateReview(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return ResponseEntity.ok(queryUseCase.getAllReviews());
    }
}