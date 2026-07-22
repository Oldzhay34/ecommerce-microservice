package com.cart.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("domain.model")
public class Cart {
    private Long id;
    private UUID userId;
    private List<CartItem> items;
    private BigDecimal totalAmount;

    public Cart() {
        this.items = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
    }

    public Cart(Long id, UUID userId, List<CartItem> items, BigDecimal totalAmount) {
        this.id = id;
        this.userId = userId;
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    // --- SADECE GETTER VE SETTER'LAR ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}