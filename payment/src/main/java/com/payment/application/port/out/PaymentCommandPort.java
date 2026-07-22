package com.payment.application.port.out;

import com.payment.domain.model.Payment;

public interface PaymentCommandPort {
    Payment save(Payment payment);

    /**
     * Bir sipariş için daha önce ödeme kaydı oluşturulmuş mu? Yazma modeli (güçlü
     * tutarlı) üzerinden sorgulanır; tekrarlanan order.approved olaylarında ikinci
     * kez tahsilat yapılmasını engellemek için kullanılır.
     */
    boolean existsPaymentForOrder(java.util.UUID orderId);
    void saveOutboxEvent(String aggregateType, String aggregateId, String type, String payload);
}