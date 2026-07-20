package com.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@org.springframework.modulith.NamedInterface("dto")
public class OrderItemDto {

    @NotBlank(message = "Product ID boş olamaz")
    private String productId;

    @NotBlank(message = "Store ID boş olamaz")
    private String storeId;

    @NotNull(message = "Miktar boş olamaz")
    @Min(value = 1, message = "Miktar en az 1 olmalıdır")
    private Integer quantity;

    @NotNull(message = "Fiyat boş olamaz")
    @Min(value = 0, message = "Fiyat negatif olamaz")
    private BigDecimal price;

    public OrderItemDto() {}

    public OrderItemDto(String productId, String storeId, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.storeId = storeId;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}