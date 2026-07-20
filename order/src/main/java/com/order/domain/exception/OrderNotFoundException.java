package com.order.domain.exception;

@org.springframework.modulith.NamedInterface("exception")
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}