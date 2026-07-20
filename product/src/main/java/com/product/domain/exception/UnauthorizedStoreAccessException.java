package com.product.domain.exception;

// Saf Domain Exception: Bir mağazanın başka bir mağazanın ürününü güncellemesini engeller
public class UnauthorizedStoreAccessException extends RuntimeException {
    public UnauthorizedStoreAccessException(String message) {
        super(message);
    }
}