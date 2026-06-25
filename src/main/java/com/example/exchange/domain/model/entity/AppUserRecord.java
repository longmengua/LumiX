/*
 * File purpose: Local exchange user account record for first-party registration and login.
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.Account;

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

    // Email verification blocks login for real customer registrations until mailbox ownership is proven.
    @Column
    private Instant emailVerifiedAt;

    // Raw verification tokens are never stored; this field stores SHA-256 hex only.
    @Column(length = 64)
    private String emailVerificationTokenHash;

    @Column
    private Instant emailVerificationExpiresAt;

    // Roles/scopes are stored as space-delimited strings to match the existing JWT authenticator contract.
    @Column(nullable = false, length = 255)
    private String roles = "USER";

    @Column(nullable = false, length = 255)
    private String scopes = "trade funds:write user:read";

    // Preferred UI language is captured at registration and updated whenever the signed-in customer switches locale.
    @Column(nullable = false, length = 16)
    private String preferredLanguage = "en";

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

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public String getEmailVerificationTokenHash() {
        return emailVerificationTokenHash;
    }

    public Instant getEmailVerificationExpiresAt() {
        return emailVerificationExpiresAt;
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

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean isPendingEmailVerification() {
        return "PENDING_EMAIL_VERIFICATION".equals(status);
    }

    /** Moves a new customer into pending verification and stores only the token hash. */
    public void startEmailVerification(String tokenHash, Instant expiresAt) {
        status = "PENDING_EMAIL_VERIFICATION";
        emailVerifiedAt = null;
        emailVerificationTokenHash = tokenHash;
        emailVerificationExpiresAt = expiresAt;
    }

    /** Activates the customer after the verification link proves mailbox ownership. */
    public void verifyEmail(Instant verifiedAt) {
        status = "ACTIVE";
        emailVerifiedAt = verifiedAt;
        emailVerificationTokenHash = null;
        emailVerificationExpiresAt = null;
    }

    /** Stores only supported frontend locale keys so downstream email/UI rendering can trust the value. */
    public void updatePreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = normalizePreferredLanguage(preferredLanguage);
    }

    /** Email comparison is case-insensitive for login and uniqueness checks. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public static String normalizePreferredLanguage(String preferredLanguage) {
        if (preferredLanguage == null) {
            return "en";
        }
        return switch (preferredLanguage.trim()) {
            case "zh-TW", "ms", "ko" -> preferredLanguage.trim();
            default -> "en";
        };
    }
}
