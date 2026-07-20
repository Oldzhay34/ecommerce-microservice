package com.promptengineering.auth.domain.model;
import java.util.UUID;

public class Customer extends User {
    private String shippingAddress;
    private String phoneNumber;
    private int loyaltyPoints;

    public Customer(UUID id, String name, String email, String passwordHash, boolean isVerified,
                    String shippingAddress, String phoneNumber, int loyaltyPoints) {
        super(id, name, email, passwordHash, "ROLE_CUSTOMER", isVerified);
        this.shippingAddress = shippingAddress;
        this.phoneNumber = phoneNumber;
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }
}