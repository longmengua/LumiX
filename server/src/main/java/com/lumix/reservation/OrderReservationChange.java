package com.lumix.reservation;

import java.math.BigDecimal;

/**
 * 撮合／撤單服務的內部資金命令；所有識別碼必須由受驗證的訂單解析，不直接接受 browser 的帳戶指派。
 *
 * <p>同時帶 owner、account、order 與 reservation，是為了在資料庫重新核對完整用途，避免成交誤扣其他
 * 訂單或提款的 hold。金額只允許既有 ledger 可保存的 NUMERIC(36,18) 範圍。</p>
 */
public record OrderReservationChange(
        Action action, String ownerUserId, String accountId, String orderId,
        String reservationId, String assetSymbol, BigDecimal amount,
        String requestId, String idempotencyKey
) {
    /** CAPTURE 只解除對應 locked，必須與成交 ledger debit 在同一個外層交易提交。 */
    public enum Action { CAPTURE, RELEASE }

    public OrderReservationChange {
        if (action == null) throw new IllegalArgumentException("action is required");
        ownerUserId = identifier(ownerUserId, 64);
        accountId = identifier(accountId, 64);
        orderId = identifier(orderId, 64);
        reservationId = identifier(reservationId, 64);
        assetSymbol = identifier(assetSymbol, 32);
        requestId = identifier(requestId, 64);
        idempotencyKey = identifier(idempotencyKey, 128);
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        // 先限制原始 precision／scale，避免極端 exponent 在 canonicalization 時放大字串或計算成本。
        if (amount.precision() > 36 || amount.scale() < 0 || amount.scale() > 18
                || amount.precision() - amount.scale() > 18) {
            throw new IllegalArgumentException("amount exceeds ledger precision");
        }
        amount = amount.stripTrailingZeros();
        if (amount.scale() < 0) amount = amount.setScale(0);
    }

    private static String identifier(String value, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum
                || !value.equals(value.trim()) || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("invalid order reservation identifier");
        }
        return value;
    }
}
