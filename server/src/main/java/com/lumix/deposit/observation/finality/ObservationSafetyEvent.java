package com.lumix.deposit.observation.finality;

/**
 * 會令 network observation pipeline fail-closed 的事件類別。
 */
public enum ObservationSafetyEvent {
    NONE,
    REORG_DETECTED,
    CONFIRMATION_REGRESSION,
    CURSOR_GAP,
    STALE_PROVIDER_SIGNAL
}
