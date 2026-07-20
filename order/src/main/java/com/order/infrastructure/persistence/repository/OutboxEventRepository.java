package com.order.infrastructure.persistence.repository;

import com.order.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@org.springframework.modulith.NamedInterface("infrastructure.repository")
public interface OutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, String> {
    List<OutboxEventJpaEntity> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}