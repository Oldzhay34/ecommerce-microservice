package com.payment.infrastructure.persistence.repository;

import com.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    /**
     * Idempotency kontrolü. Bilerek YAZMA MODELİ (PostgreSQL) üzerinden sorgulanır:
     * okuma modeli olan Elasticsearch near-real-time'dır (varsayılan refresh 1 sn) ve
     * aynı order.approved olayı hızlı tekrar geldiğinde henüz görünür olmayabilir.
     */
    boolean existsByOrderId(UUID orderId);
}