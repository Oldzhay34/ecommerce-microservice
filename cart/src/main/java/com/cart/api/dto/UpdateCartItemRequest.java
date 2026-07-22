package com.cart.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@org.springframework.modulith.NamedInterface("dto")
public class UpdateCartItemRequest {

    // Kurallara göre miktar 0 yapılırsa ürün sepetten çıkarılır, bu yüzden minimum değer 0 kabul edilebilir.
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    public UpdateCartItemRequest() {
    }

    public UpdateCartItemRequest(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}