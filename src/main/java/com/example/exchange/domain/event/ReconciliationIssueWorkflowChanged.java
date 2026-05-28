/*
 * 檔案用途：領域事件，記錄 reconciliation issue workflow 狀態變更。
 */
package com.example.exchange.domain.event;

import java.time.Instant;

public record ReconciliationIssueWorkflowChanged(
        long issueId,
        String reportId,
        String action,
        String status,
        String owner,
        Instant changedAt
) {
}
