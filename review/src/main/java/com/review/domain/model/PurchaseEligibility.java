package com.review.domain.model;

public class PurchaseEligibility {
    private String id;
    private String orderId;
    private String customerId;
    private String productId;
    private String status; // PENDING_REVIEW, REVIEWED

    public PurchaseEligibility() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}