package com.payment.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private String status;

    public PaymentResponse() {
    }

    public PaymentResponse(UUID id, UUID orderId, UUID customerId, BigDecimal amount, String status) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}