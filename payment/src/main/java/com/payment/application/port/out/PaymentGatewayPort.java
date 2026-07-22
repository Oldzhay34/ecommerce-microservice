package com.payment.application.port.out;

public interface PaymentGatewayPort {
    PaymentGatewayResult charge(PaymentChargeCommand command);
}