package com.payment.infrastructure.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.UUID;

@Document(indexName = "payments")
public class PaymentDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private UUID orderId;

    @Field(type = FieldType.Keyword)
    private UUID customerId;

    @Field(type = FieldType.Double)
    private BigDecimal amount;

    @Field(type = FieldType.Keyword)
    private String status;

    public PaymentDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}