package com.product.application.port.in;

import com.product.domain.model.Product;
import java.util.List;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("port.in")
public interface ProductQueryUseCase {
    List<Product> search(String keyword, String category);
    Product findById(UUID id);
}