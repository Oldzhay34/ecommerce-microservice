package com.cart.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID; // EKLENDİ

@org.springframework.modulith.NamedInterface("dto")
public class AddToCartRequest {
    @NotNull
    private UUID productId; // Long yerine UUID

    // BUG FIX: Sadece @Min(1) vardı. Bean Validation'da @Min null değeri GEÇERLİ
    // sayar; bu yüzden quantity alanı gönderilmediğinde istek doğrulamadan geçip
    // CartCommandUseCaseImpl'de NullPointerException'a (HTTP 500) dönüşüyordu.
    // Sözleşme gereği eksik miktar 400 Bad Request olmalı.
    @NotNull(message = "Quantity cannot be null")
    @Min(1)
    private Integer quantity;

    @NotNull
    private BigDecimal price;

    public AddToCartRequest() {}

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}