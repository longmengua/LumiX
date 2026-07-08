package com.lumix.ledger.domain;

import com.lumix.account.AssetSymbol;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ledger journal 的純 domain invariant policy。
 *
 * 這個 policy 只做 posting 前檢查，不寫資料庫、不開 transaction，也不改 balance。
 * 任何把這個 policy 接到正式 runtime 的變更都必須走 HUMAN_REVIEW_REQUIRED。
 */
public class LedgerInvariantPolicy {

    /**
     * 檢查 journal draft 是否符合雙錄帳的最基本 invariant。
     *
     * 回傳空集合代表通過；只要有任何 violation，就代表這份 journal draft 不能進入後續 posting flow。
     */
    public List<LedgerInvariantViolation> validate(LedgerJournalDraft journalDraft) {
        Objects.requireNonNull(journalDraft, "journalDraft must not be null");

        List<LedgerInvariantViolation> violations = new ArrayList<>();
        validateEntrySequences(journalDraft, violations);
        validatePerAssetBalance(journalDraft, violations);

        return List.copyOf(violations);
    }

    private void validateEntrySequences(LedgerJournalDraft journalDraft, List<LedgerInvariantViolation> violations) {
        // entrySequence 必須穩定、可重建且不重複，否則 journal 會失去 deterministic ordering。
        Set<Long> seenSequences = new LinkedHashSet<>();
        for (LedgerEntryDraft entry : journalDraft.entries()) {
            if (entry.entrySequence() <= 0L) {
                violations.add(new LedgerInvariantViolation(
                        "ENTRY_SEQUENCE_NOT_POSITIVE",
                        "entrySequence must be greater than zero"
                ));
            }
            if (!seenSequences.add(entry.entrySequence())) {
                violations.add(new LedgerInvariantViolation(
                        "ENTRY_SEQUENCE_DUPLICATED",
                        "entrySequence must be unique within a journal"
                ));
            }
        }
    }

    private void validatePerAssetBalance(LedgerJournalDraft journalDraft, List<LedgerInvariantViolation> violations) {
        // 雙錄帳要按 asset 分開平衡，不能用跨 asset 加總去偷過關。
        Map<AssetSymbol, BigDecimal> debitTotals = new LinkedHashMap<>();
        Map<AssetSymbol, BigDecimal> creditTotals = new LinkedHashMap<>();

        for (LedgerEntryDraft entry : journalDraft.entries()) {
            Map<AssetSymbol, BigDecimal> target = entry.direction() == LedgerDirection.DEBIT ? debitTotals : creditTotals;
            target.merge(entry.assetSymbol(), entry.amount(), BigDecimal::add);
        }

        Set<AssetSymbol> assetSymbols = new LinkedHashSet<>();
        assetSymbols.addAll(debitTotals.keySet());
        assetSymbols.addAll(creditTotals.keySet());

        for (AssetSymbol assetSymbol : assetSymbols) {
            BigDecimal debitTotal = debitTotals.getOrDefault(assetSymbol, BigDecimal.ZERO);
            BigDecimal creditTotal = creditTotals.getOrDefault(assetSymbol, BigDecimal.ZERO);
            if (debitTotal.compareTo(creditTotal) != 0) {
                violations.add(new LedgerInvariantViolation(
                        "ASSET_NOT_BALANCED",
                        "Debit total and credit total must match for each asset: " + assetSymbol.value()
                ));
            }
        }
    }
}
