package com.lumix.ledger.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 ledger journal draft 只在 domain contract 層檢查 invariant，不碰任何 runtime posting。
 */
class LedgerInvariantPolicyTest {

    private final LedgerInvariantPolicy policy = new LedgerInvariantPolicy();

    /**
     * 確認一份有效的 journal draft 可以通過 invariant 檢查。
     *
     * 這個 case 必須存在，因為 domain contract 要能接受合法雙錄帳，不是只會拒絕。
     */
    @Test
    void validJournalDraftPassesInvariantCheck() {
        LedgerJournalDraft journalDraft = new LedgerJournalDraft(
                LedgerBusinessReferenceType.ORDER,
                "order-1001",
                List.of(
                        new LedgerEntryDraft(new AccountId("acct-debit"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                new BigDecimal("120.50"), 1L),
                        new LedgerEntryDraft(new AccountId("acct-credit"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                new BigDecimal("120.50"), 2L),
                        new LedgerEntryDraft(new AccountId("acct-btc-debit"), new AssetSymbol("BTC"), LedgerDirection.DEBIT,
                                new BigDecimal("0.2500"), 3L),
                        new LedgerEntryDraft(new AccountId("acct-btc-credit"), new AssetSymbol("BTC"), LedgerDirection.CREDIT,
                                new BigDecimal("0.2500"), 4L)
                )
        );

        assertTrue(policy.validate(journalDraft).isEmpty());
    }

    /**
     * 確認 amount 小於等於 0 時，draft 自身就會拒絕。
     *
     * 這是最基本的資金保護，不能把零金額或負金額放進 journal draft。
     */
    @Test
    void amountLessThanOrEqualToZeroIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LedgerEntryDraft(
                new AccountId("acct-1"),
                new AssetSymbol("USDT"),
                LedgerDirection.DEBIT,
                BigDecimal.ZERO,
                1L
        ));

        assertThrows(IllegalArgumentException.class, () -> new LedgerEntryDraft(
                new AccountId("acct-1"),
                new AssetSymbol("USDT"),
                LedgerDirection.CREDIT,
                new BigDecimal("-1"),
                1L
        ));
    }

    /**
     * 確認每個 asset 都必須各自平衡，不能拿不同 asset 互相抵銷。
     *
     * 這個 case 直接保護雙錄帳 invariant：asset 維度不能跨幣別混算。
     */
    @Test
    void differentAssetsMustEachBalanceIndependently() {
        LedgerJournalDraft journalDraft = new LedgerJournalDraft(
                LedgerBusinessReferenceType.TRADE,
                "trade-2002",
                List.of(
                        new LedgerEntryDraft(new AccountId("acct-usdt-debit"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                new BigDecimal("50"), 1L),
                        new LedgerEntryDraft(new AccountId("acct-usdt-credit"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                new BigDecimal("50"), 2L),
                        new LedgerEntryDraft(new AccountId("acct-btc-debit"), new AssetSymbol("BTC"), LedgerDirection.DEBIT,
                                new BigDecimal("0.10"), 3L),
                        new LedgerEntryDraft(new AccountId("acct-btc-credit"), new AssetSymbol("BTC"), LedgerDirection.CREDIT,
                                new BigDecimal("0.08"), 4L)
                )
        );

        List<LedgerInvariantViolation> violations = policy.validate(journalDraft);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(violation ->
                "ASSET_NOT_BALANCED".equals(violation.ruleCode())
                        && violation.message().contains("BTC")));
    }

    /**
     * 確認 debit / credit 不平衡時，policy 會明確回報 violation。
     *
     * 這是 ledger journal 最核心的不變式，不能在 posting 前被忽略。
     */
    @Test
    void debitCreditImbalanceFails() {
        LedgerJournalDraft journalDraft = new LedgerJournalDraft(
                LedgerBusinessReferenceType.FEE,
                "fee-3003",
                List.of(
                        new LedgerEntryDraft(new AccountId("acct-fee-debit"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                new BigDecimal("10"), 1L),
                        new LedgerEntryDraft(new AccountId("acct-fee-credit"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                new BigDecimal("9"), 2L)
                )
        );

        List<LedgerInvariantViolation> violations = policy.validate(journalDraft);
        assertFalse(violations.isEmpty());
        assertEquals("ASSET_NOT_BALANCED", violations.get(0).ruleCode());
    }

    /**
     * 確認 entrySequence 重複時會失敗。
     *
     * deterministic sequence 是 journal 可稽核的重要條件，不能允許重複。
     */
    @Test
    void duplicatedEntrySequenceFails() {
        LedgerJournalDraft journalDraft = new LedgerJournalDraft(
                LedgerBusinessReferenceType.ADJUSTMENT,
                "adj-4004",
                List.of(
                        new LedgerEntryDraft(new AccountId("acct-a"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                new BigDecimal("1"), 1L),
                        new LedgerEntryDraft(new AccountId("acct-b"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                new BigDecimal("1"), 1L)
                )
        );

        List<LedgerInvariantViolation> violations = policy.validate(journalDraft);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(violation -> "ENTRY_SEQUENCE_DUPLICATED".equals(violation.ruleCode())));
    }

    /**
     * 確認 entrySequence 小於等於 0 時，draft 自身就會拒絕。
     *
     * 這個限制保護 journal ordering，避免出現無法排序的 entry。
     */
    @Test
    void entrySequenceLessThanOrEqualToZeroIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LedgerEntryDraft(
                new AccountId("acct-1"),
                new AssetSymbol("USDT"),
                LedgerDirection.DEBIT,
                new BigDecimal("1"),
                0L
        ));

        assertThrows(IllegalArgumentException.class, () -> new LedgerEntryDraft(
                new AccountId("acct-1"),
                new AssetSymbol("USDT"),
                LedgerDirection.CREDIT,
                new BigDecimal("1"),
                -1L
        ));
    }
}
