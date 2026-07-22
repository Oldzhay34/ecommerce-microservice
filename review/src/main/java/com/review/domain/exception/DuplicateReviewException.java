package com.review.domain.exception;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(String message) { super(message); }
}