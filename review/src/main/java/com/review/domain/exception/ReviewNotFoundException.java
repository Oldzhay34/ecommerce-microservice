package com.review.domain.exception;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(String message) { super(message); }
}