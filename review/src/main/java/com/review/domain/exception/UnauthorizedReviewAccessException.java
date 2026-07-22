package com.review.domain.exception;

public class UnauthorizedReviewAccessException extends RuntimeException {
    public UnauthorizedReviewAccessException(String message) { super(message); }
}