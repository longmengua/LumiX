/*
 * 檔案用途：hedge execution worker lock DTO。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeExecutionLock {

    private final String lockName;

    private final String ownerId;

    private final Instant expiresAt;

    private final Instant updatedAt;
    public HedgeExecutionLock(String lockName, String ownerId, Instant expiresAt, Instant updatedAt) {
        this.lockName = lockName;
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
    }

    public String lockName() {
        return lockName;
    }

    public String ownerId() {
        return ownerId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}