package com.order.infrastructure.persistence.repository;

import com.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.modulith.NamedInterface("infrastructure.repository")
public interface OrderRepository extends JpaRepository<OrderJpaEntity, String> {
}