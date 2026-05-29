/*
 * 檔案用途：Web DTO，對外回傳 matching worker 單 symbol ownership readiness。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.application.service.MatchingWorkerLifecycleService;

import java.time.Instant;

public record MatchingWorkerOwnerContextResponse(
        String symbolCode,
        String ownerId,
        long ownerEpoch,
        Instant leaseExpiresAt,
        long commandOffset,
        long eventOffset,
        Instant readyAt
) {

    /**
     * 將 application lifecycle context 轉成 Web response，避免 controller 暴露內部 record 命名。
     */
    public static MatchingWorkerOwnerContextResponse from(
            MatchingWorkerLifecycleService.MatchingWorkerOwnerContext context
    ) {
        return new MatchingWorkerOwnerContextResponse(
                context.symbolCode(),
                context.ownerId(),
                context.ownerEpoch(),
                context.leaseExpiresAt(),
                context.recoveredCommandOffset(),
                context.recoveredEventOffset(),
                context.readyAt()
        );
    }
}
