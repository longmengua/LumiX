/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.ValidationIssue;
import com.example.exchange.domain.model.dto.Account;
import com.example.exchange.domain.model.dto.Position;
import com.example.exchange.domain.model.dto.WalletLedgerEntry;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.EventStore;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final WalletLedgerRepository ledgerRepository;
    private EventStore eventStore;

    @Autowired(required = false)
    public void setEventStore(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * 掃描目前可發現的帳戶集合。
     *
     * <p>來源包含 account repository 的 maintained index，以及 open-position index。
     * 後者可抓出「有倉位但帳戶缺失」這類高風險不一致。</p>
     */
    public List<ValidationIssue> validateAllAccounts() {
        List<ValidationIssue> issues = new ArrayList<>();
        for (Long uid : discoverAccountUids()) {
            issues.addAll(validateUid(uid));
        }
        return issues;
    }

    public Set<Long> discoverAccountUids() {
        Set<Long> uids = new LinkedHashSet<>();
        accountRepository.findAll().stream()
                .filter(account -> account != null)
                .map(Account::uid)
                .forEach(uids::add);
        positionRepository.findOpenPositions().stream()
                .filter(position -> position != null)
                .map(Position::getUid)
                .forEach(uids::add);
        return uids;
    }

    /**
     * 驗證單一使用者的 Account、Position 與 Ledger 基本一致性。
     *
     * <p>這是輕量 baseline：檢查 crossBalance 分量、position margin 加總、
     * 以及每筆 ledger 是否借貸平衡。event-store coverage 仍留給 production 對帳。</p>
     */
    public List<ValidationIssue> validateUid(long uid) {
        List<ValidationIssue> issues = new ArrayList<>();
        Account account = accountRepository.findByUid(uid).orElse(null);
        if (account == null) {
            issues.add(new ValidationIssue("WARN", "ACCOUNT_MISSING", "account not found: " + uid));
            return issues;
        }

        // Account 的 cross balance 必須能被可用、委託凍結、持倉保證金完全拆解。
        BigDecimal componentTotal = account.crossAvailable()
                .add(account.crossOrderHold())
                .add(account.crossPositionMargin());
        if (account.crossBalance().compareTo(componentTotal) != 0) {
            issues.add(new ValidationIssue(
                    "ERROR",
                    "ACCOUNT_COMPONENT_MISMATCH",
                    "crossBalance must equal available + orderHold + positionMargin"
            ));
        }

        // Position margin 是帳務與持倉之間最容易漂移的欄位，先做加總比對。
        BigDecimal positionMargin = positionRepository.findAllByUid(uid).stream()
                .map(Position::getMargin)
                .map(value -> value == null ? BigDecimal.ZERO : value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (account.crossPositionMargin().compareTo(positionMargin) != 0) {
            issues.add(new ValidationIssue(
                    "ERROR",
                    "POSITION_MARGIN_MISMATCH",
                    "account position margin does not match open position margin sum"
            ));
        }

        // 每筆 ledger entry 本身必須借貸平衡，否則後續 replay / reconciliation 都不可信。
        for (WalletLedgerEntry entry : ledgerRepository.findByUid(uid)) {
            if (!entry.isBalanced()) {
                issues.add(new ValidationIssue(
                        "ERROR",
                        "LEDGER_UNBALANCED",
                        "ledger entry is not balanced: " + entry.getId()
                ));
            }
        }

        if (eventStore != null && hasOpenPosition(uid) && eventStore.lastSeq(uid) == 0) {
            issues.add(new ValidationIssue(
                    "WARN",
                    "EVENT_STORE_COVERAGE_MISSING",
                    "open position exists but event store has no replay checkpoint for uid: " + uid
            ));
        }
        return issues;
    }

    private boolean hasOpenPosition(long uid) {
        return positionRepository.findAllByUid(uid).stream()
                .anyMatch(position -> position.getQty() != null && position.getQty().signum() != 0);
    }
}
