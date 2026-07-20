package com.promptengineering.auth.domain.model;

import java.util.UUID;

public class Store extends User {
    private String storeName;
    private String taxNumber;
    private double storeRating;

    public Store(UUID id, String name, String email, String passwordHash, boolean isVerified,
                 String storeName, String taxNumber, double storeRating) {
        super(id, name, email, passwordHash, "ROLE_STORE", isVerified);
        this.storeName = storeName;
        this.taxNumber = taxNumber;
        this.storeRating = storeRating;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public double getStoreRating() {
        return storeRating;
    }
}