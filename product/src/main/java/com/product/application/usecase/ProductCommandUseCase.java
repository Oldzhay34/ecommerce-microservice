package com.product.application.usecase;

import com.product.api.dto.CreateProductRequest;
import com.product.api.dto.UpdateStockRequest;
import com.product.application.port.out.ProductCommandPort;
import com.product.domain.model.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@org.springframework.modulith.NamedInterface("usecase")
@Service
public class ProductCommandUseCase {

    private final ProductCommandPort commandPort;
    private final ObjectMapper objectMapper;

    public ProductCommandUseCase(ProductCommandPort commandPort, ObjectMapper objectMapper) {
        this.commandPort = commandPort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Product createProduct(CreateProductRequest request, UUID storeId) {
        Product product = new Product(UUID.randomUUID(), storeId, request.name(), request.category(), request.price(), request.stock());
        Product savedProduct = commandPort.saveProduct(product);

        publishOutboxEvent("Product", savedProduct.getId().toString(), "catalog.event.created", savedProduct);
        return savedProduct;
    }

    @Transactional
    public void updateStock(UUID productId, UpdateStockRequest request, UUID storeId) {
        Product product = commandPort.findProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // [Varsayım: Sadece ürünü ekleyen mağaza (STORE) stok güncelleyebilir]
        if (!product.getStoreId().equals(storeId)) {
            throw new RuntimeException("Unauthorized store access for this product");
        }

        product.setStock(request.newStock());
        Product savedProduct = commandPort.saveProduct(product);

        publishOutboxEvent("Product", savedProduct.getId().toString(), "catalog.event.updated", savedProduct);
    }

    private void publishOutboxEvent(String aggregateType, String aggregateId, String eventType, Product payloadObject) {
        try {
            String payload = objectMapper.writeValueAsString(payloadObject);
            commandPort.saveOutboxEvent(aggregateType, aggregateId, eventType, payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing outbox payload", e);
        }
    }
}