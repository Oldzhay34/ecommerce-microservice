package com.payment.application.port.in;

import com.payment.domain.model.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentQueryUseCase {
    List<Payment> getPaymentsByCustomerId(UUID customerId);
    List<Payment> getPaymentsByOrderId(UUID orderId);
    List<Payment> getAllPayments();
    Payment getPaymentByIdAndCustomerId(UUID paymentId, UUID customerId);
}
