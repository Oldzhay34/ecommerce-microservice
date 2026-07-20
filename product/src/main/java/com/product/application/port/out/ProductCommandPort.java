package com.product.application.port.out;

import com.product.domain.model.Product;
import com.product.domain.model.Review;
import java.util.Optional;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("port.out")
public interface ProductCommandPort {
    Product saveProduct(Product product);
    Optional<Product> findProductById(UUID id);
    Review saveReview(Review review);
    void saveOutboxEvent(String aggregateType, String aggregateId, String eventType, String payload);
}