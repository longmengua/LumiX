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

        // TODO(HUMAN_REVIEW_REQUIRED): 目前不做正式安全挑戰與風控審核，避免讓 stub 看起來像已通過提現保護流程。
        // TODO(HUMAN_REVIEW_REQUIRED): 目前不呼叫 LedgerService 的 reserve / release / commit，因為這些邊界尚未在本階段定義完成。
        // TODO(HUMAN_REVIEW_REQUIRED): 目前不做任何鏈上廣播或查詢，避免在沒有正式 approval 與 signing 流程前外送資金。

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
