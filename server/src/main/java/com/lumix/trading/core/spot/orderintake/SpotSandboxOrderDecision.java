package com.lumix.trading.core.spot.orderintake;

/**
 * Spot sandbox order intake 的決策結果。
 *
 * 這個 decision 只表示 intake 驗證是否通過，不代表 order 已經 persisted、reserved、matched 或 settled。
 */
public enum SpotSandboxOrderDecision {
    ACCEPTED,
    REJECTED
}
