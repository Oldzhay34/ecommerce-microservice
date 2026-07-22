package com.payment.support;

import com.payment.application.port.out.PaymentChargeCommand;
import com.payment.application.port.out.PaymentGatewayResult;
import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.payment.infrastructure.search.document.PaymentDocument;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * payment servisine ÖZEL test fixture'ları. Para söz konusu olduğu için tutarlar
 * her zaman {@code new BigDecimal("...")} (String ctor) ile üretilir: double ctor
 * ölçek/hassasiyet bozar.
 */
public final class PaymentTestFixtures {

    private PaymentTestFixtures() {
    }

    public static final BigDecimal TRY_100 = new BigDecimal("100.00");

    public static Payment payment(UUID id, UUID orderId, UUID customerId, String amount, PaymentStatus status) {
        return new Payment(id, orderId, customerId, new BigDecimal(amount), status);
    }

    public static Payment completedPayment(UUID id, UUID customerId, String amount) {
        return payment(id, UUID.randomUUID(), customerId, amount, PaymentStatus.COMPLETED);
    }

    public static Payment pendingPayment(UUID id, UUID customerId, String amount) {
        return payment(id, UUID.randomUUID(), customerId, amount, PaymentStatus.PENDING);
    }

    public static PaymentJpaEntity entity(UUID id, UUID orderId, UUID customerId, String amount, PaymentStatus status) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(id);
        entity.setOrderId(orderId);
        entity.setCustomerId(customerId);
        entity.setAmount(new BigDecimal(amount));
        entity.setStatus(status);
        return entity;
    }

    public static PaymentDocument document(UUID id, UUID orderId, UUID customerId, String amount, PaymentStatus status) {
        PaymentDocument document = new PaymentDocument();
        document.setId(id != null ? id.toString() : null);
        document.setOrderId(orderId);
        document.setCustomerId(customerId);
        document.setAmount(new BigDecimal(amount));
        document.setStatus(status != null ? status.name() : null);
        return document;
    }

    public static PaymentChargeCommand chargeCommand(UUID orderId, UUID customerId, String amount) {
        return new PaymentChargeCommand(orderId, customerId, new BigDecimal(amount), "TRY");
    }

    public static PaymentGatewayResult completedResult() {
        return new PaymentGatewayResult(PaymentStatus.COMPLETED, "TX-1", "ok");
    }

    public static PaymentGatewayResult failedResult() {
        return new PaymentGatewayResult(PaymentStatus.FAILED, "TX-2", "declined");
    }

    /** order servisinin order.approved kuyruğuna yazdığı payload biçimi. */
    public static String orderApprovedPayload(UUID orderId, UUID customerId, String amount) {
        return String.format("{\"orderId\":\"%s\", \"customerId\":\"%s\", \"amount\":%s, \"status\":\"APPROVED\"}",
                orderId, customerId, amount);
    }
}
