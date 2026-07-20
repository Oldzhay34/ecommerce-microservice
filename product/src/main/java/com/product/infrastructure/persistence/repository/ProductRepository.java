package com.product.infrastructure.persistence.repository;

import com.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductJpaEntity, UUID> {
    // Özel JPA query metodları buraya eklenebilir (Örn: findByStoreId)
}