package com.payment.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentChargeCommand(
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency
) {}