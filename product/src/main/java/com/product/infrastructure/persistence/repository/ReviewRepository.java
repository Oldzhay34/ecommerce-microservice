package com.product.infrastructure.persistence.repository;

import com.product.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    List<ReviewJpaEntity> findByProductId(UUID productId);
}