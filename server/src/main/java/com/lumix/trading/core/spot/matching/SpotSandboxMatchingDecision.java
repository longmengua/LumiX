package com.lumix.trading.core.spot.matching;

/**
 * Spot sandbox matching design 的狀態標籤。
 *
 * 這些標籤只描述設計與邊界成熟度，不代表正式 matching runtime 已可上線。
 */
public enum SpotSandboxMatchingDecision {
    DESIGN_ONLY,
    RUNTIME_NOT_IMPLEMENTED,
    HUMAN_REVIEW_REQUIRED
}
