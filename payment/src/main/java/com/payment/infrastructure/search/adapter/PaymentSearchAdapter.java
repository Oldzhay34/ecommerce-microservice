package com.payment.infrastructure.search.adapter;

import com.payment.application.port.out.PaymentQueryPort;
import com.payment.domain.model.Payment;
import com.payment.infrastructure.search.mapper.PaymentDocumentMapper;
import com.payment.infrastructure.search.repository.PaymentSearchRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PaymentSearchAdapter implements PaymentQueryPort {

    private final PaymentSearchRepository paymentSearchRepository;
    private final PaymentDocumentMapper documentMapper;

    public PaymentSearchAdapter(PaymentSearchRepository paymentSearchRepository, PaymentDocumentMapper documentMapper) {
        this.paymentSearchRepository = paymentSearchRepository;
        this.documentMapper = documentMapper;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentSearchRepository.findById(id.toString())
                .map(documentMapper::toDomain);
    }

    @Override
    public List<Payment> findByCustomerId(UUID customerId) {
        return paymentSearchRepository.findByCustomerId(customerId).stream()
                .map(documentMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByOrderId(UUID orderId) {
        return paymentSearchRepository.findByOrderId(orderId).stream()
                .map(documentMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Payment> findAll() {
        List<Payment> payments = new ArrayList<>();
        paymentSearchRepository.findAll().forEach(doc -> payments.add(documentMapper.toDomain(doc)));
        return payments;
    }
}