package com.review.domain.exception;

public class ReviewNotEligibleException extends RuntimeException {
    public ReviewNotEligibleException(String message) { super(message); }
}