package com.lumix.wallet;

import com.lumix.account.UserId;
import com.lumix.common.BusinessException;
import com.lumix.common.ErrorCode;
import com.lumix.ledger.LedgerService;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Phase 10 的提現 stub。
 * 只做 request validation 與流程占位，不做真實風控、凍結、鏈上廣播或持久化。
 */
public class DefaultWithdrawService implements WithdrawService {

    private final LedgerService ledgerService;

    public DefaultWithdrawService(LedgerService ledgerService) {
        this.ledgerService = Objects.requireNonNull(ledgerService, "ledgerService must not be null");
    }

    @Override
    public WithdrawRecord submitWithdrawal(WithdrawRecord withdrawRecord) {
        Objects.requireNonNull(withdrawRecord, "withdrawRecord must not be null");
        validateRecord(withdrawRecord);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. Future implementation must enforce security checks such as email, SMS, or authenticator challenges.

        // TODO: requires high-reasoning review before production use
        // Placeholder only. Future implementation must enforce withdraw risk review before any asset operation.

        // TODO: requires high-reasoning review before production use
        // Placeholder only. Future implementation should use LedgerService reserve / release / commit boundaries.
        // This stub intentionally does not call ledgerService.reserve(...), release(...), or commit(...).

        // TODO: requires high-reasoning review before production use
        // Placeholder only. Future implementation may hand off to WalletGateway after approval.
        // This stub intentionally does not broadcast or query any chain transaction.

        return new WithdrawRecord(
                withdrawRecord.id(),
                withdrawRecord.requestId(),
                withdrawRecord.userId(),
                withdrawRecord.asset(),
                withdrawRecord.chain(),
                withdrawRecord.address(),
                withdrawRecord.amount(),
                withdrawRecord.fee(),
                null,
                WithdrawStatus.SUBMITTED,
                withdrawRecord.createdAt(),
                Instant.now()
        );
    }

    @Override
    public List<WithdrawRecord> listWithdrawals(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return List.of();
    }

    private void validateRecord(WithdrawRecord withdrawRecord) {
        if (!withdrawRecord.amount().isPositive()) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "Withdraw amount must be greater than zero");
        }
        if (withdrawRecord.fee() != null && withdrawRecord.fee().isNegative()) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "Withdraw fee must not be negative");
        }
        if (withdrawRecord.status() != WithdrawStatus.SUBMITTED
                && withdrawRecord.status() != WithdrawStatus.RISK_REVIEW
                && withdrawRecord.status() != WithdrawStatus.ADMIN_REVIEW) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Withdraw submission stub only accepts SUBMITTED, RISK_REVIEW, or ADMIN_REVIEW status"
            );
        }
    }
}
