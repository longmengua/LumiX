package com.lumix.user.auth.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 使用者可檢視的成功登入時間投影。
 *
 * <p>此值只來自伺服器端 session 建立時間；不把 session secret、session identifier、IP 或 user agent
 * 放進使用者 API，以避免把認證材料或未經隱私治理的追蹤資料暴露出去。</p>
 */
public record LoginHistoryEntry(Instant occurredAt) {

    public LoginHistoryEntry {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
