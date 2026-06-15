/*
 * 檔案用途：訊息中心 WebSocket 通知封包，給前端即時增量更新列表。
 */
package com.example.exchange.interfaces.web.dto;

import java.time.Instant;

public record MessageCenterWebSocketEvent(
        String messageId,
        String title,
        String summary,
        String category,
        String severity,
        Instant createdAt,
        boolean isScheduled,
        String actionUrl,
        String actionLabel
) {
}
