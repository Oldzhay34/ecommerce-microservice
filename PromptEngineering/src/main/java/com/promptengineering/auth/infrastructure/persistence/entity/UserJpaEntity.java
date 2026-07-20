package com.promptengineering.auth.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class UserJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    // AES-256 şifreleme bu converter üzerinden şeffaf olarak gerçekleşir.
    @Column(name = "name")
    @Convert(converter = AesEncryptionConverter.class)
    private String name;

    @Column(name = "email_encrypted", unique = true)
    @Convert(converter = AesEncryptionConverter.class)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "role_name")
    private String role;

    @Column(name = "is_verified")
    private boolean isVerified;

    public UserJpaEntity() {}

    public UserJpaEntity(UUID id, String name, String email, String passwordHash, String role, boolean isVerified) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isVerified = isVerified;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean isVerified) { this.isVerified = isVerified; }
}