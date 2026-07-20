package com.product.application.port.in;

import com.product.api.dto.CreateProductRequest;
import com.product.api.dto.UpdateStockRequest;
import com.product.domain.model.Product;

import java.util.UUID;

@org.springframework.modulith.NamedInterface("port.in")
public interface ProductCommandUseCase {
    Product createProduct(CreateProductRequest request, UUID storeId);
    void updateStock(UUID productId, UpdateStockRequest request, UUID storeId);
}