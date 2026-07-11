package com.lumix.trading.core.projection;

/**
 * balance projection runtime 的設計能力。
 *
 * 這些能力只描述未來 read model 的目標，不代表目前已有 runtime。
 */
public enum BalanceProjectionCapability {
    READ_MODEL_ONLY,
    REBUILD_FROM_LEDGER,
    REPLAY_FROM_LEDGER,
    RECONCILE_WITH_LEDGER,
    OBSERVE_LAG
}
