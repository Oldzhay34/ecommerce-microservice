package com.product.domain.model;

import java.util.UUID;

@org.springframework.modulith.NamedInterface("model")
public class Review {
    private UUID id;
    private UUID productId;
    private UUID customerId;
    private String comment;
    private Integer rating;

    public Review(UUID id, UUID productId, UUID customerId, String comment, Integer rating) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.comment = comment;
        this.rating = rating;
    }
    public Review(){

    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}