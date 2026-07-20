package com.product.infrastructure.persistence.mapper;

import com.product.domain.model.Product;
import com.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductEntityMapper {

    public Product toDomain(ProductJpaEntity entity) {
        if (entity == null) return null;
        return new Product(
                entity.getId(),
                entity.getStoreId(),
                entity.getName(),
                entity.getCategory(),
                entity.getPrice(),
                entity.getStock()
        );
    }

    public ProductJpaEntity toEntity(Product domain) {
        if (domain == null) return null;
        return new ProductJpaEntity(
                domain.getId(),
                domain.getStoreId(),
                domain.getName(),
                domain.getCategory(),
                domain.getPrice(),
                domain.getStock()
        );
    }
}