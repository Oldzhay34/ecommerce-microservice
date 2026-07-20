package com.promptengineering.auth.domain.model;

import java.util.UUID;

public class Admin extends User {
    private String department;
    private String accessLevel; // "SUPER_ADMIN", "MODERATOR" vb.

    public Admin(UUID id, String name, String email, String passwordHash, boolean isVerified,
                 String department, String accessLevel) {
        super(id, name, email, passwordHash, "ROLE_ADMIN", isVerified);
        this.department = department;
        this.accessLevel = accessLevel;
    }

    public String getDepartment() {
        return department;
    }

    public String getAccessLevel() {
        return accessLevel;
    }
    // Getter & Setter...
}