package com.payment.infrastructure.messaging.listener;

import com.payment.application.port.in.PaymentCommandUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OrderApprovedEventListener {

    private final PaymentCommandUseCase paymentCommandUseCase;

    public OrderApprovedEventListener(PaymentCommandUseCase paymentCommandUseCase) {
        this.paymentCommandUseCase = paymentCommandUseCase;
    }

    @RabbitListener(queues = "order.approved.queue")
    public void handleOrderApprovedEvent(String payload) {
        // [Varsayım: JSON parse işlemi için ek kütüphane bağımlılığı yerine basit regex veya
        // manuel parse kullanıldığı varsayılmıştır (hızlı simülasyon amaçlı). Gerçek senaryoda Jackson ObjectMapper kullanılır.]
        try {
            UUID orderId = extractUuid(payload, "orderId");
            UUID customerId = extractUuid(payload, "customerId");
            BigDecimal amount = extractBigDecimal(payload, "amount");

            paymentCommandUseCase.processOrderApproved(orderId, customerId, amount);
        } catch (Exception e) {
            // [Varsayım: Dead Letter Queue (DLQ) mekanizması yapılandırılacaktır. Şimdilik loglanıp geçiliyor.]
            System.err.println("Error processing OrderApprovedEvent: " + e.getMessage());
        }
    }

    private UUID extractUuid(String payload, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
        if (matcher.find()) return UUID.fromString(matcher.group(1));
        throw new IllegalArgumentException(field + " not found in payload");
    }

    private BigDecimal extractBigDecimal(String payload, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*([0-9.]+)").matcher(payload);
        if (matcher.find()) return new BigDecimal(matcher.group(1));
        throw new IllegalArgumentException(field + " not found in payload");
    }
}