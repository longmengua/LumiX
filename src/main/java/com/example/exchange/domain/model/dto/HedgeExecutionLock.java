/*
 * 檔案用途：hedge execution worker lock DTO。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

public record HedgeExecutionLock(
        String lockName,
        String ownerId,
        Instant expiresAt,
        Instant updatedAt
) {
}
