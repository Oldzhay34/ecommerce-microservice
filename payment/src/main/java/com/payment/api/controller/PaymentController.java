package com.payment.api.controller;

import com.payment.api.dto.PaymentResponse;
import com.payment.api.dto.RefundRequest;
import com.payment.application.port.in.PaymentCommandUseCase;
import com.payment.application.port.in.PaymentQueryUseCase;
import com.payment.domain.model.Payment;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentCommandUseCase paymentCommandUseCase;
    private final PaymentQueryUseCase paymentQueryUseCase;

    public PaymentController(PaymentCommandUseCase paymentCommandUseCase, PaymentQueryUseCase paymentQueryUseCase) {
        this.paymentCommandUseCase = paymentCommandUseCase;
        this.paymentQueryUseCase = paymentQueryUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication authentication) {
        UUID customerId = UUID.fromString(authentication.getName());
        List<Payment> payments = paymentQueryUseCase.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(payments.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STORE', 'ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getPayments(@RequestParam(required = false) UUID orderId, Authentication authentication) {
        List<Payment> payments;
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            payments = paymentQueryUseCase.getAllPayments();
        } else if (orderId != null) {
            payments = paymentQueryUseCase.getPaymentsByOrderId(orderId);
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(payments.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/refund-request")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> requestRefund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request, Authentication authentication) {
        UUID customerId = UUID.fromString(authentication.getName());
        paymentCommandUseCase.requestRefund(id, customerId, request.getReason());
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/{id}/refund-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveRefund(@PathVariable UUID id) {
        paymentCommandUseCase.approveRefund(id);
        return ResponseEntity.ok().build();
    }

    // Domain -> DTO Manuel Mapping Method (Controller Level)
    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getStatus() != null ? payment.getStatus().name() : null
        );
    }
}