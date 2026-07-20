package com.product.infrastructure.search.mapper;

import com.product.domain.model.Product;
import com.product.infrastructure.search.document.ProductDocument;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

    public Product toDomain(ProductDocument document) {
        if (document == null) return null;
        return new Product(
                document.getId(),
                document.getStoreId(),
                document.getName(),
                document.getCategory(),
                document.getPrice(),
                document.getStock()
        );
    }

    public ProductDocument toDocument(Product domain) {
        if (domain == null) return null;
        return new ProductDocument(
                domain.getId(),
                domain.getStoreId(),
                domain.getName(),
                domain.getCategory(),
                domain.getPrice(),
                domain.getStock()
        );
    }
}