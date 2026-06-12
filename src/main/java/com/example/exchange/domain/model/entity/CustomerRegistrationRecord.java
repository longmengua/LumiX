/*
 * File purpose: Pending customer registration request decoupled from finalized app user accounts.
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
@Table(name = "customer_registration_requests")
public class CustomerRegistrationRecord {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Email is normalized before storage so code verification is case-insensitive.
    @Column(nullable = false, length = 320)
    private String email;

    // Pending password hash is promoted into app_users only after mailbox ownership is verified.
    @Column(nullable = false, length = 255)
    private String passwordHash;

    // Backup email-link token is separate from the primary code table so registration material stays focused.
    @Column(nullable = false, length = 64)
    private String verificationTokenHash;

    @Column(nullable = false, length = 32)
    private String status = STATUS_PENDING;

    // Captures the browser language at registration time before an app_users row exists.
    @Column(nullable = false, length = 16)
    private String preferredLanguage = "en";

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant verifiedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CustomerRegistrationRecord() {
    }

    public CustomerRegistrationRecord(
            String email,
            String passwordHash,
            String verificationTokenHash,
            Instant expiresAt,
            String preferredLanguage
    ) {
        this.email = AppUserRecord.normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.verificationTokenHash = verificationTokenHash;
        this.expiresAt = expiresAt;
        this.preferredLanguage = AppUserRecord.normalizePreferredLanguage(preferredLanguage);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getVerificationTokenHash() {
        return verificationTokenHash;
    }

    public String getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    /** Pending registration requests become unusable after the 24-hour verification window. */
    public void expire() {
        status = STATUS_EXPIRED;
    }

    /** Marks the request consumed so the same code/link cannot create multiple accounts. */
    public void verify(Instant now) {
        status = STATUS_VERIFIED;
        verifiedAt = now;
    }
}
