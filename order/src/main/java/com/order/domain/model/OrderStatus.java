package com.order.domain.model;

@org.springframework.modulith.NamedInterface("model")
public enum OrderStatus {
    PENDING,
    APPROVED,
    SHIPPED,
    CANCELLED
}