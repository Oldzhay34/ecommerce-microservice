package com.payment.infrastructure.persistence.adapter;

import com.payment.application.port.out.PaymentCommandPort;
import com.payment.domain.model.Payment;
import com.payment.infrastructure.persistence.entity.OutboxJpaEntity;
import com.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.payment.infrastructure.persistence.mapper.PaymentEntityMapper;
import com.payment.infrastructure.persistence.repository.OutboxRepository;
import com.payment.infrastructure.persistence.repository.PaymentRepository;
// Elasticsearch için eklenen importlar
import com.payment.infrastructure.search.document.PaymentDocument;
import com.payment.infrastructure.search.mapper.PaymentDocumentMapper;
import com.payment.infrastructure.search.repository.PaymentSearchRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentPersistenceAdapter implements PaymentCommandPort {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final PaymentEntityMapper mapper;

    // Elasticsearch bağımlılıkları eklendi
    private final PaymentSearchRepository paymentSearchRepository;
    private final PaymentDocumentMapper documentMapper;

    public PaymentPersistenceAdapter(PaymentRepository paymentRepository,
                                     OutboxRepository outboxRepository,
                                     PaymentEntityMapper mapper,
                                     PaymentSearchRepository paymentSearchRepository,
                                     PaymentDocumentMapper documentMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.mapper = mapper;
        this.paymentSearchRepository = paymentSearchRepository;
        this.documentMapper = documentMapper;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        // 1. PostgreSQL'e kaydet (Write Model)
        PaymentJpaEntity entity = mapper.toEntity(payment);
        PaymentJpaEntity savedEntity = paymentRepository.save(entity);
        Payment savedDomain = mapper.toDomain(savedEntity);

        // 2. Elasticsearch'ü de hemen güncelle (Read Model Sync)
        PaymentDocument document = documentMapper.toDocument(savedDomain);
        paymentSearchRepository.save(document);

        return savedDomain;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsPaymentForOrder(java.util.UUID orderId) {
        return paymentRepository.existsByOrderId(orderId);
    }

    @Override
    public void saveOutboxEvent(String aggregateType, String aggregateId, String type, String payload) {
        OutboxJpaEntity outboxEntity = new OutboxJpaEntity(aggregateType, aggregateId, type, payload);
        outboxRepository.save(outboxEntity);
    }
}