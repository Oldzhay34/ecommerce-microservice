package com.product.api.controller;

import com.product.api.dto.CreateProductRequest;
import com.product.api.dto.UpdateStockRequest;
import com.product.application.usecase.ProductCommandUseCase;
import com.product.application.usecase.ProductQueryUseCase;
import com.product.domain.model.Product;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductCommandUseCase commandUseCase;
    private final ProductQueryUseCase queryUseCase;

    public ProductController(ProductCommandUseCase commandUseCase, ProductQueryUseCase queryUseCase) {
        this.commandUseCase = commandUseCase;
        this.queryUseCase = queryUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('STORE')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductRequest request) {
        UUID storeId = extractUserIdFromSecurityContext();
        Product created = commandUseCase.createProduct(request, storeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('STORE')")
    public ResponseEntity<Void> updateStock(@PathVariable UUID id, @Valid @RequestBody UpdateStockRequest request) {
        UUID storeId = extractUserIdFromSecurityContext();
        commandUseCase.updateStock(id, request, storeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        List<Product> products = queryUseCase.search(keyword, category);
        return ResponseEntity.ok(products);
    }

    private UUID extractUserIdFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getPrincipal().toString()); // JwtResourceFilter içindeki principal UUID'dir.
    }
    // ProductController.java
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        Product product = queryUseCase.findById(id);
        return ResponseEntity.ok(product);
    }
}