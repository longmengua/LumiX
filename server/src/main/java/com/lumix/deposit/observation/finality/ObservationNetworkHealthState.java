package com.lumix.deposit.observation.finality;

/**
 * 單一 chain network 的觀測健康狀態；halt 不會自動轉回 healthy。
 */
public enum ObservationNetworkHealthState {
    HEALTHY,
    HALTED_REORG,
    HALTED_CONFIRMATION_REGRESSION,
    HALTED_CURSOR_GAP,
    HALTED_STALE
}
