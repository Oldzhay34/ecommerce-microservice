package com.payment.application.port.out;

import com.payment.domain.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentQueryPort {
    Optional<Payment> findById(UUID id);
    List<Payment> findByCustomerId(UUID customerId);
    List<Payment> findByOrderId(UUID orderId);
    List<Payment> findAll();
}