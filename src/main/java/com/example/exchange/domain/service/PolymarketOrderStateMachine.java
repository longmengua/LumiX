/*
 * 檔案用途：Polymarket local/CLOB order 狀態轉換 guard。
 */
package com.example.exchange.domain.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PolymarketOrderStateMachine {

    private static final Set<String> ACTIVE_REMOTE_STATUSES =
            Set.of("LIVE", "MATCHED", "ORDER_STATUS_LIVE", "ORDER_STATUS_MATCHED");

    private static final Set<String> TERMINAL_STATUSES =
            Set.of(
                    "CANCELED",
                    "CANCELLED",
                    "ORDER_STATUS_CANCELED",
                    "ORDER_STATUS_CANCELLED",
                    "FILLED",
                    "ORDER_STATUS_FILLED",
                    "SETTLED",
                    "ORDER_STATUS_SETTLED",
                    "FAILED",
                    "REJECTED"
            );

    public String resolveRemoteStatus(String currentStatus, String remoteStatus) {
        String normalizedRemote =
                normalize(remoteStatus);
        if (normalizedRemote == null) {
            return currentStatus;
        }

        String normalizedCurrent =
                normalize(currentStatus);
        if (normalizedCurrent != null
                && TERMINAL_STATUSES.contains(normalizedCurrent)
                && ACTIVE_REMOTE_STATUSES.contains(normalizedRemote)) {
            return currentStatus;
        }

        return remoteStatus;
    }

    public boolean shouldApplyRemoteMatchedSize(String currentStatus, String remoteStatus) {
        String normalizedCurrent =
                normalize(currentStatus);
        String normalizedRemote =
                normalize(remoteStatus);
        return normalizedCurrent == null
                || normalizedRemote == null
                || !TERMINAL_STATUSES.contains(normalizedCurrent)
                || !ACTIVE_REMOTE_STATUSES.contains(normalizedRemote);
    }

    private static String normalize(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase();
    }
}
