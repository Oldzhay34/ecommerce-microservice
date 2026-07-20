package com.order.domain.model;

import java.math.BigDecimal;

@org.springframework.modulith.NamedInterface("model")
public class OrderItem {
    private String id;
    private String productId;
    private String storeId;
    private Integer quantity;
    private BigDecimal price;

    public OrderItem() {}

    public OrderItem(String id, String productId, String storeId, Integer quantity, BigDecimal price) {
        this.id = id;
        this.productId = productId;
        this.storeId = storeId;
        this.quantity = quantity;
        this.price = price;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}