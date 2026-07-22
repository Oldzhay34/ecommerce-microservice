package com.payment.infrastructure.gateway.dev;

import com.payment.application.port.out.PaymentChargeCommand;
import com.payment.application.port.out.PaymentGatewayPort;
import com.payment.application.port.out.PaymentGatewayResult;
import com.payment.domain.model.PaymentStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.gateway.provider", havingValue = "dev", matchIfMissing = true)
public class DevPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final BigDecimal LIMIT = new BigDecimal("10000");

    @Override
    public PaymentGatewayResult charge(PaymentChargeCommand command) {
        boolean isSuccess = command.amount().compareTo(LIMIT) <= 0;
        PaymentStatus status = isSuccess ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
        String transactionId = "DEV-" + UUID.randomUUID().toString();
        String message = isSuccess ? "Simulated success" : "Amount exceeds dev limit";

        return new PaymentGatewayResult(status, transactionId, message);
    }
}