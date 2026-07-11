package com.lumix.trading.core.spot.orderintake;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Spot sandbox order intake 的 in-memory boundary。
 *
 * 這個 boundary 只做 command validation 與 accepted / rejected 判定，不代表正式 order placement runtime。
 */
public final class SpotSandboxOrderIntakeBoundary {

    private final SpotSandboxOrderIntakePolicy policy = new SpotSandboxOrderIntakePolicy();

    /**
     * 受理或拒絕 sandbox order command。
     *
     * 這裡只做 domain-level intake validation，不會持久化、也不會接 reservation / matching / settlement。
     */
    public SpotSandboxOrderIntakeResult evaluate(SpotSandboxOrderCommand command) {
        if (command == null) {
            return reject(null, SpotSandboxOrderRejectionReason.MISSING_REQUEST_ID, "sandbox order command 不可為 null");
        }

        if (isBlank(command.requestId())) {
            return reject(command, SpotSandboxOrderRejectionReason.MISSING_REQUEST_ID, "requestId 必須存在，且只作 trace / correlation");
        }
        if (isBlank(command.idempotencyKey())) {
            return reject(command, SpotSandboxOrderRejectionReason.MISSING_IDEMPOTENCY_KEY, "idempotencyKey 必須存在，但本題不做 store / lookup");
        }
        if (isBlank(command.userId())) {
            return reject(command, SpotSandboxOrderRejectionReason.MISSING_USER_ID, "userId 必須存在");
        }
        if (isBlank(command.accountId())) {
            return reject(command, SpotSandboxOrderRejectionReason.MISSING_ACCOUNT_ID, "accountId 必須存在");
        }
        if (isBlank(command.marketSymbol())) {
            return reject(command, SpotSandboxOrderRejectionReason.MISSING_MARKET_SYMBOL, "marketSymbol 必須非空");
        }
        if (command.side() == null) {
            return reject(command, SpotSandboxOrderRejectionReason.MISSING_SIDE, "side 必須是 BUY 或 SELL");
        }
        if (command.type() == null) {
            return reject(command, SpotSandboxOrderRejectionReason.MISSING_ORDER_TYPE, "type 必須存在");
        }
        if (command.type() != SpotOrderType.LIMIT) {
            return reject(command, SpotSandboxOrderRejectionReason.UNSUPPORTED_ORDER_TYPE, "目前只支援 LIMIT order");
        }
        if (command.timeInForce() == null) {
            return reject(command, SpotSandboxOrderRejectionReason.MISSING_TIME_IN_FORCE, "timeInForce 必須存在");
        }
        if (command.timeInForce() != SpotTimeInForce.GTC) {
            return reject(command, SpotSandboxOrderRejectionReason.UNSUPPORTED_TIME_IN_FORCE, "目前只支援 GTC");
        }
        if (!isPositive(command.price())) {
            return reject(command, SpotSandboxOrderRejectionReason.INVALID_PRICE, "price 必須大於 0");
        }
        if (!isPositive(command.quantity())) {
            return reject(command, SpotSandboxOrderRejectionReason.INVALID_QUANTITY, "quantity 必須大於 0");
        }

        return SpotSandboxOrderIntakeResult.accepted(command);
    }

    /**
     * 回傳 intake policy。
     *
     * 這個方法只提供設計契約閱讀，不代表 boundary 已經接上任何 persistence runtime。
     */
    public SpotSandboxOrderIntakePolicy policy() {
        return policy;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static SpotSandboxOrderIntakeResult reject(
            SpotSandboxOrderCommand command,
            SpotSandboxOrderRejectionReason reason,
            String message
    ) {
        return SpotSandboxOrderIntakeResult.rejected(command, new SpotSandboxOrderRejection(reason, message));
    }
}
