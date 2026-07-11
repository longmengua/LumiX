package com.lumix.trading.core.spot;

/**
 * Spot sandbox runtime 的狀態標籤。
 *
 * 這些標籤只描述設計與邊界成熟度，不代表正式交易 runtime 已可上線。
 */
public enum SpotSandboxRuntimeStatus {
    DESIGN_ONLY,
    SANDBOX_BOUNDARY_DEFINED,
    RUNTIME_NOT_IMPLEMENTED,
    HUMAN_REVIEW_REQUIRED
}
