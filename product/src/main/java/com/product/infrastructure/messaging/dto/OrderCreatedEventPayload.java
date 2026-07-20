package com.product.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order servisinin "OrderCreatedEvent" payload'ını (Order domain objesinin
 * tamamı) deserialize etmek için kullanılan DTO.
 *
 * KONTRAT: order-service com.order.domain.model.Order sınıfının JSON
 * çıktısıyla alan bazında birebir eşleşmelidir. Order tarafında bu sınıf
 * değişirse burası da güncellenmelidir (bkz. Pact contract test önerisi).
 */
public class OrderCreatedEventPayload {

    private String id;
    private String userId;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItemPayload> items;

    public OrderCreatedEventPayload() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItemPayload> getItems() { return items; }
    public void setItems(List<OrderItemPayload> items) { this.items = items; }

    public static class OrderItemPayload {
        private String id;
        private String productId;
        private String storeId;
        private Integer quantity;
        private BigDecimal price;

        public OrderItemPayload() {}

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
}