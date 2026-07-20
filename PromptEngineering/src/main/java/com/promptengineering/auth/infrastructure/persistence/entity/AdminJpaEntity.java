package com.promptengineering.auth.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class AdminJpaEntity extends UserJpaEntity {

    @Column(name = "department")
    private String department;

    @Column(name = "access_level")
    private String accessLevel;

    public AdminJpaEntity() {
        super();
    }

    // --- GETTER & SETTER ---

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
}