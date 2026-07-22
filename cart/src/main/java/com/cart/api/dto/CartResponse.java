package com.cart.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID; // EKLENDİ

@org.springframework.modulith.NamedInterface("dto")
public class CartResponse {
    private UUID userId; // Long yerine UUID oldu
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;

    public CartResponse() {}

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}