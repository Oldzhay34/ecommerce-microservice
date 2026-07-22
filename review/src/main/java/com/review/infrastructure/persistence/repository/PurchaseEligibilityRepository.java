package com.review.infrastructure.persistence.repository;

import com.review.infrastructure.persistence.entity.PurchaseEligibilityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PurchaseEligibilityRepository extends JpaRepository<PurchaseEligibilityJpaEntity, String> {
    Optional<PurchaseEligibilityJpaEntity> findByOrderIdAndCustomerIdAndProductId(String orderId, String customerId, String productId);
}