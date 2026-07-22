package com.cart.infrastructure.persistence.repository;

import com.cart.infrastructure.persistence.entity.CartJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@org.springframework.modulith.NamedInterface("infrastructure.repository")
public interface CartRepository extends JpaRepository<CartJpaEntity, Long> {

    /**
     * BUG FIX: CartJpaEntity.items @OneToMany (varsayılan LAZY). CartEntityMapper
     * toDomain() sırasında koleksiyona dokunuyor; sorgu açık bir persistence
     * context dışında çağrıldığında (ör. testten doğrudan adapter çağrısı)
     * LazyInitializationException fırlıyordu, transaction içinde ise her sepet
     * için ek bir SELECT (N+1) atılıyordu. @EntityGraph ile items tek sorguda
     * eager olarak getirilir.
     */
    @EntityGraph(attributePaths = "items")
    Optional<CartJpaEntity> findByUserId(UUID userId);
}