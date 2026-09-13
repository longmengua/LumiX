package com.lumix.admin.user;

import java.time.Instant;

/** 裝置摘要只供支援人員判讀使用者綁定狀態；不傳回 token、IP 或 user-agent digest。 */
public record AdminUserDevice(String platform, String label, Instant lastSeenAt) { }
