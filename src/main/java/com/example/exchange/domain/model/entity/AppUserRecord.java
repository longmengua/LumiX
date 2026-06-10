/*
 * File purpose: Local exchange user account record for first-party registration and login.
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUserRecord {

    // UID is reused as the exchange account uid, so registration creates both user and Account records.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Normalized lowercase email is the first-party login identifier for the MVP auth flow.
    @Column(nullable = false, unique = true, length = 320)
    private String email;

    // Password hashes use PBKDF2 format metadata; raw passwords are never persisted.
    @Column(nullable = false, length = 255)
    private String passwordHash;

    // Status allows future lock/disable flows without deleting the user or historical audit records.
    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    // Roles/scopes are stored as space-delimited strings to match the existing JWT authenticator contract.
    @Column(nullable = false, length = 255)
    private String roles = "USER";

    @Column(nullable = false, length = 255)
    private String scopes = "trade funds:write user:read";

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AppUserRecord() {
    }

    /** Creates a first-party user record from already-validated registration input. */
    public AppUserRecord(String email, String passwordHash) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
    }

    /** Keeps audit timestamps local to persistence so service code cannot forget them. */
    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Updates the modification timestamp for future profile/status changes. */
    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public String getRoles() {
        return roles;
    }

    public String getScopes() {
        return scopes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /** Email comparison is case-insensitive for login and uniqueness checks. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
