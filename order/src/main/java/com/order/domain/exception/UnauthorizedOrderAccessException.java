package com.order.domain.exception;

@org.springframework.modulith.NamedInterface("exception")
public class UnauthorizedOrderAccessException extends RuntimeException {
    public UnauthorizedOrderAccessException(String message) {
        super(message);
    }
}