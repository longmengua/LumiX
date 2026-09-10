package com.lumix.user.auth.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 使用者可檢視的成功登入安全快照。
 *
 * <p>此值只來自伺服器端 session 建立時的不可變快照；不含 session secret、session identifier 或完整
 * User-Agent。IP 與友善裝置標籤只會回傳給同一個已驗證使用者，供其識別異常登入。</p>
 */
public record LoginHistoryEntry(Instant occurredAt, String ipAddress, String deviceLabel) {

    public LoginHistoryEntry {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
