package com.product.application.port.out;

import com.product.domain.model.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("port.out")
public interface ProductQueryPort {
    List<Product> searchProducts(String keyword, String category);
    Product saveToSearch(Product product); // SyncListener için kullanılacak

    Optional<Product> findById(UUID id);
}