/*
 * File purpose: Independent customer email verification code state for registration and future resend flows.
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
@Table(name = "customer_verification_codes")
public class CustomerVerificationCodeRecord {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Email is the stable lookup key before an app_users row exists.
    @Column(nullable = false, length = 320)
    private String email;

    // Nullable until a pending registration is promoted into a finalized app user.
    @Column
    private Long appUserId;

    // Optional association to registration; future admin resend flows can issue account-level codes without it.
    @Column
    private Long registrationRequestId;

    // Raw codes are never stored; the hash includes normalized email to reduce cross-account replay risk.
    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false, length = 32)
    private String status = STATUS_PENDING;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant verifiedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CustomerVerificationCodeRecord() {
    }

    public CustomerVerificationCodeRecord(
            String email,
            Long appUserId,
            Long registrationRequestId,
            String codeHash,
            Instant expiresAt
    ) {
        this.email = AppUserRecord.normalizeEmail(email);
        this.appUserId = appUserId;
        this.registrationRequestId = registrationRequestId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
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

    public Long getAppUserId() {
        return appUserId;
    }

    public Long getRegistrationRequestId() {
        return registrationRequestId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    /** Expired codes cannot be reused by later manual resend or registration completion attempts. */
    public void expire() {
        status = STATUS_EXPIRED;
    }

    /** Consumes the code and optionally links it to the finalized account created by registration. */
    public void verify(Instant now, Long appUserId) {
        status = STATUS_VERIFIED;
        verifiedAt = now;
        this.appUserId = appUserId;
    }
}
