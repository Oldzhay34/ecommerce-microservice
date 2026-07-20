package com.promptengineering.auth.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class StoreJpaEntity extends UserJpaEntity {

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "tax_number", unique = true)
    private String taxNumber;

    @Column(name = "store_rating")
    private double storeRating;

    public StoreJpaEntity() {
        super();
    }

    // --- GETTER & SETTER ---

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public double getStoreRating() { return storeRating; }
    public void setStoreRating(double storeRating) { this.storeRating = storeRating; }
}