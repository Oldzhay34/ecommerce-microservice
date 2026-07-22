package com.cart.api.dto;

import java.math.BigDecimal;
import java.util.UUID; // EKLENDİ

@org.springframework.modulith.NamedInterface("dto")
public class CartItemResponse {

    // DÜZELTME: Long -> UUID
    private UUID productId;
    private Integer quantity;
    private BigDecimal price;

    public CartItemResponse() {
    }

    // DÜZELTME: Long -> UUID
    public CartItemResponse(UUID productId, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    // DÜZELTME: Long -> UUID
    public UUID getProductId() {
        return productId;
    }

    // DÜZELTME: Long -> UUID
    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}