package com.payment.infrastructure.persistence.repository;

import com.payment.infrastructure.persistence.entity.OutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxJpaEntity, UUID> {
}