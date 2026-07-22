package com.review.api.dto;

import java.time.LocalDateTime;

public class ReviewResponse {
    private String id;
    private String productId;
    private String customerId;
    private int rating;
    private String comment;
    private String status;
    private String storeReplyText;
    private LocalDateTime storeRepliedAt;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStoreReplyText() { return storeReplyText; }
    public void setStoreReplyText(String storeReplyText) { this.storeReplyText = storeReplyText; }
    public LocalDateTime getStoreRepliedAt() { return storeRepliedAt; }
    public void setStoreRepliedAt(LocalDateTime storeRepliedAt) { this.storeRepliedAt = storeRepliedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}