package com.lumix.trading.core.spot.orderintake;

/**
 * Spot sandbox order intake 的拒絕細節。
 *
 * 這裡只保存可安全回報的 reason code 與 message，不保存任何 SQL、stack trace 或敏感資訊。
 */
public record SpotSandboxOrderRejection(
        SpotSandboxOrderRejectionReason reason,
        String message
) {
}
