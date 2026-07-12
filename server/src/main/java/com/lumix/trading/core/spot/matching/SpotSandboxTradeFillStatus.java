package com.lumix.trading.core.spot.matching;

/**
 * Spot sandbox trade / fill result 的狀態。
 *
 * 這些狀態只代表 sandbox trade/fill 及其 settlement input 的設計狀態，不代表任何正式 settlement 已完成。
 */
public enum SpotSandboxTradeFillStatus {
    CREATED_FOR_SANDBOX,
    SETTLEMENT_NOT_STARTED,
    HUMAN_REVIEW_REQUIRED
}
