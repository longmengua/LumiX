/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.ValidationIssue;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final WalletLedgerRepository ledgerRepository;

    public List<ValidationIssue> validateUid(long uid) {
        List<ValidationIssue> issues = new ArrayList<>();
        Account account = accountRepository.findByUid(uid).orElse(null);
        if (account == null) {
            issues.add(new ValidationIssue("WARN", "ACCOUNT_MISSING", "account not found: " + uid));
            return issues;
        }

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

        for (WalletLedgerEntry entry : ledgerRepository.findByUid(uid)) {
            if (!entry.isBalanced()) {
                issues.add(new ValidationIssue(
                        "ERROR",
                        "LEDGER_UNBALANCED",
                        "ledger entry is not balanced: " + entry.getId()
                ));
            }
        }
        return issues;
    }
}
