package com.lumix.reservation;

import java.math.BigDecimal;

/**
 * 單次命令完成時的持久化金額快照；冪等重播回傳原結果，不將它冒充後續交易後的即時餘額。
 */
public record OrderReservationChangeResult(
        BigDecimal remainingAmount, BigDecimal consumedAmount, BigDecimal releasedAmount,
        String status, boolean replayed
) {
    public OrderReservationChangeResult {
        if (remainingAmount == null || consumedAmount == null || releasedAmount == null
                || remainingAmount.signum() < 0 || consumedAmount.signum() < 0 || releasedAmount.signum() < 0 || status == null
                || !java.util.Set.of("ACTIVE", "PARTIALLY_CONSUMED", "CONSUMED", "RELEASED").contains(status)) {
            throw new IllegalArgumentException("invalid order reservation result evidence");
        }
        boolean terminal = "CONSUMED".equals(status) || "RELEASED".equals(status);
        if (terminal != (remainingAmount.signum() == 0)
                || ("CONSUMED".equals(status) && consumedAmount.signum() == 0)
                || ("RELEASED".equals(status) && releasedAmount.signum() == 0)
                || (!terminal && "PARTIALLY_CONSUMED".equals(status) != (consumedAmount.signum() > 0))) {
            throw new IllegalArgumentException("order reservation result status conflicts with amounts");
        }
    }

    /** 保留原金額與狀態，只指出此次呼叫沒有再度異動資金。 */
    public OrderReservationChangeResult asReplay() {
        return new OrderReservationChangeResult(remainingAmount, consumedAmount, releasedAmount, status, true);
    }
}
