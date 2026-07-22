package com.payment.application.port.out;

import com.payment.domain.model.PaymentStatus;

public record PaymentGatewayResult(
        PaymentStatus status,
        String providerTransactionId,
        String rawResponseMessage
) {}