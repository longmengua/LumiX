package com.lumix.trading.core.futures.margin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.common.MoneyAmount;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 margin check result 的 decision / reason / calculation invariant 不會互相矛盾。
 */
class IsolatedMarginCheckResultTest {

    /**
     * 確認 approved result 只能表達足夠保證金，且必須攜帶已計算的 notional。
     */
    @Test
    void approvedFactoryCreatesValidApprovedResult() {
        IsolatedMarginCheckResult result = IsolatedMarginCheckResult.approved(
                amount("25000"),
                amount("25000.000")
        );

        assertEquals(FuturesMarginCheckStatus.APPROVED, result.status());
        assertEquals(FuturesMarginCheckReason.SUFFICIENT_MARGIN, result.reason());
        assertTrue(result.requestedNotional().isPresent());
        assertTrue(result.marginSupportedNotional().isPresent());
        assertEquals(amount("25000"), result.requestedNotional().orElseThrow());
        assertEquals(amount("25000"), result.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認 insufficient-margin rejection 仍要攜帶比較所需的計算值。
     */
    @Test
    void insufficientMarginFactoryCreatesValidRejection() {
        IsolatedMarginCheckResult result = IsolatedMarginCheckResult.insufficientMargin(
                amount("25000"),
                amount("24999.99")
        );

        assertEquals(FuturesMarginCheckStatus.REJECTED, result.status());
        assertEquals(FuturesMarginCheckReason.INSUFFICIENT_MARGIN, result.reason());
        assertEquals(amount("25000"), result.requestedNotional().orElseThrow());
        assertEquals(amount("24999.99"), result.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認一致性失敗的 rejection 不會偽造未執行的計算值。
     *
     * 這個 case 必須存在，因為 account / market / asset mismatch 應先停在安全邊界，不應產生誤導性的 notional。
     */
    @Test
    void consistencyRejectionCarriesNoCalculationOutcome() {
        IsolatedMarginCheckResult result = IsolatedMarginCheckResult.rejected(FuturesMarginCheckReason.MARKET_MISMATCH);

        assertEquals(FuturesMarginCheckStatus.REJECTED, result.status());
        assertEquals(FuturesMarginCheckReason.MARKET_MISMATCH, result.reason());
        assertTrue(result.requestedNotional().isEmpty());
        assertTrue(result.marginSupportedNotional().isEmpty());
    }

    /**
     * 確認 approved 與 rejected 的 reason 組合不能互相混用。
     */
    @Test
    void constructorRejectsInconsistentDecisionReasonPairs() {
        IllegalArgumentException approvedWrongReason = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckResult(
                        FuturesMarginCheckStatus.APPROVED,
                        FuturesMarginCheckReason.INSUFFICIENT_MARGIN,
                        Optional.of(amount("100")),
                        Optional.of(amount("200"))
                )
        );
        assertEquals("APPROVED status must use SUFFICIENT_MARGIN reason", approvedWrongReason.getMessage());

        IllegalArgumentException rejectedSufficient = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckResult(
                        FuturesMarginCheckStatus.REJECTED,
                        FuturesMarginCheckReason.SUFFICIENT_MARGIN,
                        Optional.of(amount("100")),
                        Optional.of(amount("200"))
                )
        );
        assertEquals("SUFFICIENT_MARGIN reason must use APPROVED status", rejectedSufficient.getMessage());
    }

    /**
     * 確認 approved / insufficient-margin 的數值邊界不能互相矛盾。
     */
    @Test
    void constructorRejectsContradictingCalculationOutcomes() {
        IllegalArgumentException approvedTooSmall = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckResult(
                        FuturesMarginCheckStatus.APPROVED,
                        FuturesMarginCheckReason.SUFFICIENT_MARGIN,
                        Optional.of(amount("100")),
                        Optional.of(amount("99.99"))
                )
        );
        assertEquals("APPROVED result requires supported notional greater than or equal to requested notional", approvedTooSmall.getMessage());

        IllegalArgumentException insufficientTooLarge = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckResult(
                        FuturesMarginCheckStatus.REJECTED,
                        FuturesMarginCheckReason.INSUFFICIENT_MARGIN,
                        Optional.of(amount("100")),
                        Optional.of(amount("100.00"))
                )
        );
        assertEquals("INSUFFICIENT_MARGIN result requires supported notional less than requested notional", insufficientTooLarge.getMessage());
    }

    /**
     * 確認 canonical constructor 不接受 null 與不完整 optional。
     *
     * 這個 case 必須存在，因為 T04 明確禁止用 null 表示「未計算」。
     */
    @Test
    void constructorRejectsNullAndHalfPresentCalculations() {
        assertThrows(
                NullPointerException.class,
                () -> new IsolatedMarginCheckResult(null, FuturesMarginCheckReason.SUFFICIENT_MARGIN, Optional.of(amount("1")), Optional.of(amount("1")))
        );
        assertThrows(
                NullPointerException.class,
                () -> new IsolatedMarginCheckResult(FuturesMarginCheckStatus.APPROVED, null, Optional.of(amount("1")), Optional.of(amount("1")))
        );
        assertThrows(
                NullPointerException.class,
                () -> new IsolatedMarginCheckResult(FuturesMarginCheckStatus.REJECTED, FuturesMarginCheckReason.MARKET_MISMATCH, null, Optional.empty())
        );
        assertThrows(
                NullPointerException.class,
                () -> new IsolatedMarginCheckResult(FuturesMarginCheckStatus.REJECTED, FuturesMarginCheckReason.MARKET_MISMATCH, Optional.empty(), null)
        );

        IllegalArgumentException halfPresent = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckResult(
                        FuturesMarginCheckStatus.REJECTED,
                        FuturesMarginCheckReason.MARKET_MISMATCH,
                        Optional.of(amount("1")),
                        Optional.empty()
                )
        );
        assertEquals("requestedNotional and marginSupportedNotional must both be present or both be empty", halfPresent.getMessage());
    }

    /**
     * 確認 notional 值本身也有明確邊界。
     */
    @Test
    void constructorRejectsInvalidNotionalBounds() {
        IllegalArgumentException requestedNotPositive = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckResult(
                        FuturesMarginCheckStatus.APPROVED,
                        FuturesMarginCheckReason.SUFFICIENT_MARGIN,
                        Optional.of(MoneyAmount.zero()),
                        Optional.of(amount("1"))
                )
        );
        assertEquals("requestedNotional must be greater than zero", requestedNotPositive.getMessage());

        IllegalArgumentException supportedNegative = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckResult(
                        FuturesMarginCheckStatus.REJECTED,
                        FuturesMarginCheckReason.INSUFFICIENT_MARGIN,
                        Optional.of(amount("1")),
                        Optional.of(amount("-0.01"))
                )
        );
        assertEquals("marginSupportedNotional must not be negative", supportedNegative.getMessage());
    }

    /**
     * 確認 direct constructor 不能繞過「一致性失敗不得帶計算值」的 invariant。
     */
    @Test
    void directConstructorCannotBypassConsistencyRejectionInvariant() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckResult(
                        FuturesMarginCheckStatus.REJECTED,
                        FuturesMarginCheckReason.ACCOUNT_MISMATCH,
                        Optional.of(amount("100")),
                        Optional.of(amount("200"))
                )
        );

        assertEquals("consistency rejections must not include notional calculations", exception.getMessage());
    }

    /**
     * 確認 factory 會保留 compareTo semantics，而不是受 trailing zero 影響。
     */
    @Test
    void valueSemanticsIgnoreTrailingZeros() {
        IsolatedMarginCheckResult result = IsolatedMarginCheckResult.approved(
                amount("100.0"),
                amount("100.000")
        );

        assertFalse(result.requestedNotional().orElseThrow().compareTo(result.marginSupportedNotional().orElseThrow()) != 0);
    }

    private static MoneyAmount amount(String value) {
        return new MoneyAmount(new BigDecimal(value));
    }
}
