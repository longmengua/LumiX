/*
 * File purpose: Refresh-token session record for local exchange auth logout and revocation.
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_refresh_sessions")
public class AuthRefreshSessionRecord {

    // Refresh sessions are persisted so logout can revoke server-side state even when access JWTs are stateless.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links the session to app_users.id, which is also the internal exchange uid.
    @Column(nullable = false)
    private Long userId;

    // Only a token hash is stored; the raw refresh token is returned once to the client.
    @Column(nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    // Stable operator-facing session identifier for future session listing and revoke-by-device flows.
    @Column(nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant revokedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuthRefreshSessionRecord() {
    }

    /** Creates a refresh session with an already-hashed token and explicit expiry. */
    public AuthRefreshSessionRecord(Long userId, String refreshTokenHash, Instant expiresAt) {
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.sessionId = UUID.randomUUID().toString();
        this.expiresAt = expiresAt;
    }

    /** Creation time is assigned by persistence to keep service logic focused on auth decisions. */
    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    /** Logout is idempotent: an already-revoked session keeps its original revokedAt timestamp. */
    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
