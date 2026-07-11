package com.lumix.trading.core.reservation;

/**
 * reservation runtime 可能談論的操作型別。
 *
 * 這些值只描述語意邊界，不代表任何 hold / release runtime 已經可以執行。
 */
public enum ReservationOperationType {
    HOLD,
    RELEASE,
    COMMIT,
    CANCEL
}
