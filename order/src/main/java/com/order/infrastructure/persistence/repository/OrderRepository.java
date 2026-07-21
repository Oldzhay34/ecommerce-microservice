package com.order.infrastructure.persistence.repository;

import com.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@org.springframework.modulith.NamedInterface("infrastructure.repository")
public interface OrderRepository extends JpaRepository<OrderJpaEntity, String> {

    /**
     * items koleksiyonu LAZY olduğu için, çağıran taraf açık bir persistence
     * context dışındaysa getItems() LazyInitializationException fırlatır.
     * Bu sorgu items'ı aynı seferde çekerek bunu önler.
     */
    @EntityGraph(attributePaths = "items")
    List<OrderJpaEntity> findByUserId(String userId);
}