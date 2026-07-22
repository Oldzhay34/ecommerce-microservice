package com.cart.domain.exception;

@org.springframework.modulith.NamedInterface("domain.exception")
public class UnauthorizedCartAccessException extends RuntimeException {
    public UnauthorizedCartAccessException(String message) {
        super(message);
    }
}