package com.cart.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // EKLENDİ

@Entity
@Table(name = "carts")
@org.springframework.modulith.NamedInterface("infrastructure.entity")
public class CartJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId; // Tipi değiştirildi

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemJpaEntity> items = new ArrayList<>();

    public CartJpaEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    // getItems, setItems, addItem, removeItem metotları AYNI kalıyor
    public List<CartItemJpaEntity> getItems() { return items; }
    public void setItems(List<CartItemJpaEntity> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
    }

    public void addItem(CartItemJpaEntity item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItemJpaEntity item) {
        items.remove(item);
        item.setCart(null);
    }
}