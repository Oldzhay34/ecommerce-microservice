package com.payment.application.usecase;

import com.payment.application.port.in.PaymentQueryUseCase;
import com.payment.application.port.out.PaymentQueryPort;
import com.payment.domain.exception.PaymentNotFoundException;
import com.payment.domain.exception.UnauthorizedPaymentAccessException;
import com.payment.domain.model.Payment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentQueryUseCaseImpl implements PaymentQueryUseCase {

    private final PaymentQueryPort paymentQueryPort;

    public PaymentQueryUseCaseImpl(PaymentQueryPort paymentQueryPort) {
        this.paymentQueryPort = paymentQueryPort;
    }

    @Override
    public List<Payment> getPaymentsByCustomerId(UUID customerId) {
        return paymentQueryPort.findByCustomerId(customerId);
    }

    @Override
    public List<Payment> getPaymentsByOrderId(UUID orderId) {
        return paymentQueryPort.findByOrderId(orderId);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentQueryPort.findAll();
    }

    @Override
    public Payment getPaymentByIdAndCustomerId(UUID paymentId, UUID customerId) {
        Payment payment = paymentQueryPort.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new UnauthorizedPaymentAccessException("Payment does not belong to the user");
        }
        return payment;
    }
}