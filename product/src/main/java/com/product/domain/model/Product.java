package com.product.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("model")
public class Product {
    private UUID id;
    private UUID storeId;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;

    public Product(UUID id, UUID storeId, String name, String category, BigDecimal price, Integer stock) {
        this.id = id;
        this.storeId = storeId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }
    public Product() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}