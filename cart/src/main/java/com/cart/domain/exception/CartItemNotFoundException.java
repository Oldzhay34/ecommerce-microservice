package com.cart.domain.exception;

@org.springframework.modulith.NamedInterface("domain.exception")
public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(String message) {
        super(message);
    }
}