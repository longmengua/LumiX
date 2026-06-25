/*
 * 檔案用途：應用服務，從 durable wallet ledger journal replay 並驗證帳戶資產狀態。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.LedgerReplayComparisonIssue;
import com.example.exchange.domain.model.dto.LedgerReplayComparisonReport;
import com.example.exchange.domain.model.dto.LedgerTamperEvidenceReport;
import com.example.exchange.domain.model.dto.WalletLedgerReplayResult;
import com.example.exchange.domain.model.dto.Account;
import com.example.exchange.domain.model.dto.WalletLedgerEntry;
import com.example.exchange.domain.model.dto.WalletLedgerPosting;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletLedgerReplayService {

    public static final int SCHEMA_VERSION = WalletLedgerJournal.SCHEMA_VERSION;

    private static final String USER_AVAILABLE = "USER_AVAILABLE";
    private static final String USER_ORDER_HOLD = "USER_ORDER_HOLD";
    private static final String USER_POSITION_MARGIN = "USER_POSITION_MARGIN";

    private final WalletLedgerJournal journal;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public WalletLedgerReplayResult replay(long uid, String asset) {
        String normalizedAsset = normalizeAsset(asset);
        List<WalletLedgerEntry> entries = normalizedAsset == null
                ? journal.findByUid(uid)
                : journal.findByUidAndAsset(uid, normalizedAsset);
        return replay(uid, normalizedAsset, entries, true);
    }

    @Transactional(readOnly = true)
    public WalletLedgerReplayResult replayAndCompareAccount(long uid, String asset) {
        String normalizedAsset = normalizeAsset(asset);
        WalletLedgerReplayResult result = replay(uid, normalizedAsset);
        List<String> issues = new ArrayList<>(result.issues());
        accountRepository.findByUid(uid).ifPresentOrElse(
                account -> compareAccount(account, result, issues),
                () -> issues.add("ACCOUNT_NOT_FOUND")
        );
        return new WalletLedgerReplayResult(
                result.uid(),
                result.asset(),
                result.entryCount(),
                result.postingCount(),
                result.balance(),
                result.available(),
                result.orderHold(),
                result.positionMargin(),
                result.balanced() && issues.isEmpty(),
                List.copyOf(issues),
                result.replayedAt()
        );
    }

    @Transactional(readOnly = true)
    public LedgerReplayComparisonReport compareAccountDetails(long uid, String asset) {
        String normalizedAsset = normalizeAsset(asset);
        WalletLedgerReplayResult result = replay(uid, normalizedAsset);
        List<LedgerReplayComparisonIssue> issues = new ArrayList<>();
        accountRepository.findByUid(uid).ifPresentOrElse(
                account -> collectComparisonIssues(account, result, issues),
                () -> issues.add(new LedgerReplayComparisonIssue(
                        "ACCOUNT",
                        null,
                        result.balance(),
                        result.balance()
                ))
        );
        return new LedgerReplayComparisonReport(
                uid,
                normalizedAsset,
                result.balanced() && issues.isEmpty(),
                result,
                List.copyOf(issues),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public LedgerTamperEvidenceReport verifyTamperEvidence() {
        return journal.verifyTamperEvidence();
    }

    public static void validateBalancedEntry(WalletLedgerEntry entry) {
        WalletLedgerJournal.validateBalancedEntry(entry);
    }

    static WalletLedgerReplayResult replay(
            long uid,
            String asset,
            List<WalletLedgerEntry> entries,
            boolean compareNegativeBalances
    ) {
        BigDecimal available = BigDecimal.ZERO;
        BigDecimal orderHold = BigDecimal.ZERO;
        BigDecimal positionMargin = BigDecimal.ZERO;
        int postingCount = 0;
        List<String> issues = new ArrayList<>();

        for (WalletLedgerEntry entry : entries == null ? List.<WalletLedgerEntry>of() : entries) {
            if (entry == null) continue;
            if (!entry.isBalanced()) {
                issues.add("UNBALANCED_ENTRY:" + entry.getId());
            }
            if (entry.getUid() != uid) {
                issues.add("UID_MISMATCH:" + entry.getId());
            }
            for (WalletLedgerPosting posting : entry.getPostings() == null ? List.<WalletLedgerPosting>of() : entry.getPostings()) {
                if (asset != null && !asset.equalsIgnoreCase(posting.asset())) {
                    continue;
                }
                postingCount++;
                BigDecimal delta = posting.debit().subtract(posting.credit());
                switch (posting.accountCode()) {
                    case USER_AVAILABLE -> available = available.add(delta);
                    case USER_ORDER_HOLD -> orderHold = orderHold.add(delta);
                    case USER_POSITION_MARGIN -> positionMargin = positionMargin.add(delta);
                    default -> {
                    }
                }
            }
            if (compareNegativeBalances && hasNegative(available, orderHold, positionMargin)) {
                issues.add("NEGATIVE_REPLAY_BALANCE_AFTER:" + entry.getId());
            }
        }

        BigDecimal balance = available.add(orderHold).add(positionMargin);
        return new WalletLedgerReplayResult(
                uid,
                asset,
                entries == null ? 0 : entries.size(),
                postingCount,
                balance,
                available,
                orderHold,
                positionMargin,
                issues.isEmpty(),
                List.copyOf(issues),
                Instant.now()
        );
    }

    private static void compareAccount(Account account, WalletLedgerReplayResult result, List<String> issues) {
        if (account.crossBalance().compareTo(result.balance()) != 0) {
            issues.add("CROSS_BALANCE_MISMATCH:account=" + account.crossBalance() + ",replay=" + result.balance());
        }
        if (account.crossAvailable().compareTo(result.available()) != 0) {
            issues.add("CROSS_AVAILABLE_MISMATCH:account=" + account.crossAvailable() + ",replay=" + result.available());
        }
        if (account.crossOrderHold().compareTo(result.orderHold()) != 0) {
            issues.add("CROSS_ORDER_HOLD_MISMATCH:account=" + account.crossOrderHold() + ",replay=" + result.orderHold());
        }
        if (account.crossPositionMargin().compareTo(result.positionMargin()) != 0) {
            issues.add("CROSS_POSITION_MARGIN_MISMATCH:account="
                    + account.crossPositionMargin() + ",replay=" + result.positionMargin());
        }
    }

    private static void collectComparisonIssues(
            Account account,
            WalletLedgerReplayResult result,
            List<LedgerReplayComparisonIssue> issues
    ) {
        addComparisonIssue("crossBalance", account.crossBalance(), result.balance(), issues);
        addComparisonIssue("crossAvailable", account.crossAvailable(), result.available(), issues);
        addComparisonIssue("crossOrderHold", account.crossOrderHold(), result.orderHold(), issues);
        addComparisonIssue("crossPositionMargin", account.crossPositionMargin(), result.positionMargin(), issues);
    }

    private static void addComparisonIssue(
            String component,
            BigDecimal accountValue,
            BigDecimal replayValue,
            List<LedgerReplayComparisonIssue> issues
    ) {
        if (accountValue.compareTo(replayValue) == 0) {
            return;
        }
        issues.add(new LedgerReplayComparisonIssue(
                component,
                accountValue,
                replayValue,
                accountValue.subtract(replayValue)
        ));
    }

    private static boolean hasNegative(BigDecimal available, BigDecimal orderHold, BigDecimal positionMargin) {
        return available.signum() < 0 || orderHold.signum() < 0 || positionMargin.signum() < 0;
    }

    private static String normalizeAsset(String asset) {
        return asset == null || asset.isBlank() ? null : asset.trim().toUpperCase();
    }
}
