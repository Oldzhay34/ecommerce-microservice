package com.product.domain.exception;

// Saf Domain Exception
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}