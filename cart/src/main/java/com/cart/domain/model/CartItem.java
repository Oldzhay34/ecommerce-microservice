package com.cart.domain.model;

import java.math.BigDecimal;
import java.util.UUID; // EKLENDİ

@org.springframework.modulith.NamedInterface("domain.model")
public class CartItem {
    private Long id;

    // DÜZELTME: Long -> UUID
    private UUID productId;

    private Integer quantity;
    private BigDecimal price;

    public CartItem() {
    }

    // DÜZELTME: Constructor parametresi Long -> UUID
    public CartItem(Long id, UUID productId, Integer quantity, BigDecimal price) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // DÜZELTME: Dönüş tipi UUID
    public UUID getProductId() { return productId; }

    // DÜZELTME: Parametre UUID
    public void setProductId(UUID productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}