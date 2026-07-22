package com.cart.domain.exception;

@org.springframework.modulith.NamedInterface("domain.exception")
public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String message) {
        super(message);
    }
}