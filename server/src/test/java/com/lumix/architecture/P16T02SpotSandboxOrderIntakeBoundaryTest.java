package com.lumix.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.trading.core.spot.orderintake.SpotOrderSide;
import com.lumix.trading.core.spot.orderintake.SpotOrderType;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderCommand;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderDecision;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderIntakeBoundary;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderIntakeResult;
import com.lumix.trading.core.spot.orderintake.SpotSandboxOrderRejectionReason;
import com.lumix.trading.core.spot.orderintake.SpotTimeInForce;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox order intake boundary 只產生 accepted / rejected result，不會變成正式 order runtime。
 */
class P16T02SpotSandboxOrderIntakeBoundaryTest {

    private final SpotSandboxOrderIntakeBoundary boundary = new SpotSandboxOrderIntakeBoundary();

    /**
     * 確認 valid LIMIT GTC BUY order 會被接受。
     *
     * 這個 case 必須存在，因為 sandbox order intake 的第一個正向路徑必須是可重現、可審核的。
     */
    @Test
    void acceptsValidLimitGtcBuyOrder() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(validCommand(SpotOrderSide.BUY));

        assertEquals(SpotSandboxOrderDecision.ACCEPTED, result.decision());
        assertEquals(SpotOrderSide.BUY, result.command().side());
        assertEquals(SpotOrderType.LIMIT, result.command().type());
        assertEquals(SpotTimeInForce.GTC, result.command().timeInForce());
        assertTrue(result.rejection() == null);
        assertFalse(result.toString().contains("persisted"));
        assertFalse(result.toString().contains("reserved"));
        assertFalse(result.toString().contains("matched"));
        assertFalse(result.toString().contains("settled"));
        assertFalse(result.toString().contains("posted"));
    }

    /**
     * 確認 valid LIMIT GTC SELL order 會被接受。
     *
     * 這個 case 必須存在，因為 BUY / SELL 兩個方向都要能通過 intake validation。
     */
    @Test
    void acceptsValidLimitGtcSellOrder() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(validCommand(SpotOrderSide.SELL));

        assertEquals(SpotSandboxOrderDecision.ACCEPTED, result.decision());
        assertEquals(SpotOrderSide.SELL, result.command().side());
        assertTrue(result.rejection() == null);
    }

    /**
     * 確認 price <= 0 會被拒絕。
     *
     * 這個 case 必須存在，因為 price 是交易核心輸入，不能讓非正數混進 sandbox boundary。
     */
    @Test
    void rejectsNonPositivePrice() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-03",
                "idem-p16-03",
                "user-p16-03",
                "acct-p16-03",
                "BTC-USDT",
                SpotOrderSide.BUY,
                SpotOrderType.LIMIT,
                BigDecimal.ZERO,
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        ));

        assertEquals(SpotSandboxOrderDecision.REJECTED, result.decision());
        assertEquals(SpotSandboxOrderRejectionReason.INVALID_PRICE, result.rejection().reason());
    }

    /**
     * 確認 quantity <= 0 會被拒絕。
     *
     * 這個 case 必須存在，因為 quantity 不能是零或負數，否則 sandbox 訊號會失真。
     */
    @Test
    void rejectsNonPositiveQuantity() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-04",
                "idem-p16-04",
                "user-p16-04",
                "acct-p16-04",
                "BTC-USDT",
                SpotOrderSide.SELL,
                SpotOrderType.LIMIT,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                SpotTimeInForce.GTC
        ));

        assertEquals(SpotSandboxOrderDecision.REJECTED, result.decision());
        assertEquals(SpotSandboxOrderRejectionReason.INVALID_QUANTITY, result.rejection().reason());
    }

    /**
     * 確認 blank marketSymbol 會被拒絕。
     *
     * 這個 case 必須存在，因為 marketSymbol 是 sandbox order 走向後續 boundary 的最基本識別。
     */
    @Test
    void rejectsBlankMarketSymbol() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-05",
                "idem-p16-05",
                "user-p16-05",
                "acct-p16-05",
                "   ",
                SpotOrderSide.BUY,
                SpotOrderType.LIMIT,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        ));

        assertEquals(SpotSandboxOrderDecision.REJECTED, result.decision());
        assertEquals(SpotSandboxOrderRejectionReason.MISSING_MARKET_SYMBOL, result.rejection().reason());
    }

    /**
     * 確認 blank accountId / userId 會被拒絕。
     *
     * 這個 case 必須存在，因為 intake command 必須能追到對應的使用者與帳戶邊界。
     */
    @Test
    void rejectsBlankAccountAndUserIdentity() {
        SpotSandboxOrderIntakeResult blankAccount = boundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-06",
                "idem-p16-06",
                "user-p16-06",
                " ",
                "BTC-USDT",
                SpotOrderSide.BUY,
                SpotOrderType.LIMIT,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        ));
        SpotSandboxOrderIntakeResult blankUser = boundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-07",
                "idem-p16-07",
                " ",
                "acct-p16-07",
                "BTC-USDT",
                SpotOrderSide.BUY,
                SpotOrderType.LIMIT,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        ));

        assertEquals(SpotSandboxOrderDecision.REJECTED, blankAccount.decision());
        assertEquals(SpotSandboxOrderRejectionReason.MISSING_ACCOUNT_ID, blankAccount.rejection().reason());
        assertEquals(SpotSandboxOrderDecision.REJECTED, blankUser.decision());
        assertEquals(SpotSandboxOrderRejectionReason.MISSING_USER_ID, blankUser.rejection().reason());
    }

    /**
     * 確認 missing idempotencyKey 會被拒絕。
     *
     * 這個 case 必須存在，因為 requestId 不是 idempotency guarantee，不能偷拿來代替 duplicate prevention contract。
     */
    @Test
    void rejectsMissingIdempotencyKey() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-08",
                " ",
                "user-p16-08",
                "acct-p16-08",
                "BTC-USDT",
                SpotOrderSide.BUY,
                SpotOrderType.LIMIT,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        ));

        assertEquals(SpotSandboxOrderDecision.REJECTED, result.decision());
        assertEquals(SpotSandboxOrderRejectionReason.MISSING_IDEMPOTENCY_KEY, result.rejection().reason());
    }

    /**
     * 確認 MARKET order 會被拒絕為 unsupported。
     *
     * 這個 case 必須存在，因為本題只允許 LIMIT，不能把 sandbox boundary 誤升級成完整 order engine。
     */
    @Test
    void rejectsMarketOrderAsUnsupported() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-09",
                "idem-p16-09",
                "user-p16-09",
                "acct-p16-09",
                "BTC-USDT",
                SpotOrderSide.BUY,
                SpotOrderType.MARKET,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        ));

        assertEquals(SpotSandboxOrderDecision.REJECTED, result.decision());
        assertEquals(SpotSandboxOrderRejectionReason.UNSUPPORTED_ORDER_TYPE, result.rejection().reason());
    }

    /**
     * 確認 IOC 會被拒絕為 unsupported。
     *
     * 這個 case 必須存在，因為本題只允許 GTC，避免把 time-in-force 誤解成 sandbox 已支援完整撮合語意。
     */
    @Test
    void rejectsIocAsUnsupported() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(new SpotSandboxOrderCommand(
                "req-p16-10",
                "idem-p16-10",
                "user-p16-10",
                "acct-p16-10",
                "BTC-USDT",
                SpotOrderSide.SELL,
                SpotOrderType.LIMIT,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.IOC
        ));

        assertEquals(SpotSandboxOrderDecision.REJECTED, result.decision());
        assertEquals(SpotSandboxOrderRejectionReason.UNSUPPORTED_TIME_IN_FORCE, result.rejection().reason());
    }

    /**
     * 確認 accepted result 不會宣稱 persisted / reserved / matched / settled / posted。
     *
     * 這個 case 必須存在，因為 accepted 只代表 intake validation 通過，不代表任何後續 money movement 已完成。
     */
    @Test
    void acceptedResultDoesNotClaimDownstreamRuntimeCompletion() {
        SpotSandboxOrderIntakeResult result = boundary.evaluate(validCommand(SpotOrderSide.BUY));

        assertEquals(SpotSandboxOrderDecision.ACCEPTED, result.decision());
        assertFalse(result.toString().contains("persisted"));
        assertFalse(result.toString().contains("reserved"));
        assertFalse(result.toString().contains("matched"));
        assertFalse(result.toString().contains("settled"));
        assertFalse(result.toString().contains("posted"));
    }

    private static SpotSandboxOrderCommand validCommand(SpotOrderSide side) {
        return new SpotSandboxOrderCommand(
                "req-p16-01",
                "idem-p16-01",
                "user-p16-01",
                "acct-p16-01",
                "BTC-USDT",
                side,
                SpotOrderType.LIMIT,
                new BigDecimal("50000.00"),
                new BigDecimal("0.10"),
                SpotTimeInForce.GTC
        );
    }
}
