package com.payment.domain.model;

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    REFUND_REQUESTED,   // müşteri iade talep etti, admin onayı bekliyor
    FAILED,
    REFUNDED
}