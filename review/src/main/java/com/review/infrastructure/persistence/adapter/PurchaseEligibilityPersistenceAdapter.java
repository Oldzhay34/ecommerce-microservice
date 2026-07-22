package com.review.infrastructure.persistence.adapter;

import com.review.application.port.out.PurchaseEligibilityPort;
import com.review.domain.model.PurchaseEligibility;
import com.review.infrastructure.persistence.entity.PurchaseEligibilityJpaEntity;
import com.review.infrastructure.persistence.repository.PurchaseEligibilityRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class PurchaseEligibilityPersistenceAdapter implements PurchaseEligibilityPort {

    private final PurchaseEligibilityRepository repository;

    public PurchaseEligibilityPersistenceAdapter(PurchaseEligibilityRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PurchaseEligibility> findPendingEligibility(String orderId, String customerId, String productId) {
        return repository.findByOrderIdAndCustomerIdAndProductId(orderId, customerId, productId)
                .filter(e -> "PENDING_REVIEW".equals(e.getStatus()))
                .map(this::toDomain);
    }

    @Override
    public void markAsReviewed(String eligibilityId) {
        repository.findById(eligibilityId).ifPresent(e -> {
            e.setStatus("REVIEWED");
            repository.save(e);
        });
    }

    @Override
    public void createIdempotent(String orderId, String customerId, String productId) {
        Optional<PurchaseEligibilityJpaEntity> existing = repository.findByOrderIdAndCustomerIdAndProductId(orderId, customerId, productId);
        if (existing.isEmpty()) {
            PurchaseEligibilityJpaEntity entity = new PurchaseEligibilityJpaEntity();
            entity.setOrderId(orderId);
            entity.setCustomerId(customerId);
            entity.setProductId(productId);
            entity.setStatus("PENDING_REVIEW");
            repository.save(entity);
        }
    }

    private PurchaseEligibility toDomain(PurchaseEligibilityJpaEntity entity) {
        PurchaseEligibility domain = new PurchaseEligibility();
        domain.setId(entity.getId());
        domain.setOrderId(entity.getOrderId());
        domain.setCustomerId(entity.getCustomerId());
        domain.setProductId(entity.getProductId());
        domain.setStatus(entity.getStatus());
        return domain;
    }
}