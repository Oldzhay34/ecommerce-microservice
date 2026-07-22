package com.payment.domain.exception;

public class PaymentGatewayNotConfiguredException extends RuntimeException {
    public PaymentGatewayNotConfiguredException(String message) {
        super(message);
    }
}