package com.review.infrastructure.persistence.repository;

import com.review.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewJpaEntity, String> {
    List<ReviewJpaEntity> findByCustomerId(String customerId);
}