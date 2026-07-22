package com.payment.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentCommandUseCase {
    void processOrderApproved(UUID orderId, UUID customerId, BigDecimal amount);
    void requestRefund(UUID paymentId, UUID customerId, String reason);
    void approveRefund(UUID paymentId);
}
