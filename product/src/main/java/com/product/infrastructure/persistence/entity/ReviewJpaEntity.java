package com.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class ReviewJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "comment")
    private String comment;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    public ReviewJpaEntity() {}

    public ReviewJpaEntity(UUID id, UUID productId, UUID customerId, String comment, Integer rating) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.comment = comment;
        this.rating = rating;
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