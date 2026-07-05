package com.lumix.wallet;

import com.lumix.account.UserId;
import com.lumix.common.BusinessException;
import com.lumix.common.ErrorCode;
import com.lumix.idempotency.IdempotencyKey;
import com.lumix.idempotency.IdempotencyService;
import com.lumix.ledger.LedgerService;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Phase 10 的充值 stub。
 * 只做 request validation 與流程占位，不做真實掃鏈、入帳或持久化。
 */
public class DefaultDepositService implements DepositService {

    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;

    public DefaultDepositService(LedgerService ledgerService, IdempotencyService idempotencyService) {
        this.ledgerService = Objects.requireNonNull(ledgerService, "ledgerService must not be null");
        this.idempotencyService = Objects.requireNonNull(idempotencyService, "idempotencyService must not be null");
    }

    @Override
    public DepositRecord registerObservedDeposit(DepositRecord depositRecord) {
        Objects.requireNonNull(depositRecord, "depositRecord must not be null");
        validateRecord(depositRecord);

        IdempotencyKey idempotencyKey = buildIdempotencyKey(depositRecord);

        // TODO: requires high-reasoning review before production use
        // Placeholder only. Future implementation should check txHash + asset + chain idempotency via IdempotencyService.
        // This stub intentionally does not call idempotencyService.startProcessing(...).

        // TODO: requires high-reasoning review before production use
        // Placeholder only. Future implementation should call LedgerService after confirmation policy is satisfied.
        // This stub intentionally does not call ledgerService.postJournal(...).

        return new DepositRecord(
                depositRecord.id(),
                depositRecord.userId(),
                depositRecord.asset(),
                depositRecord.chain(),
                depositRecord.txHash(),
                depositRecord.address(),
                depositRecord.amount(),
                depositRecord.confirmations(),
                resolveStubStatus(depositRecord.status(), depositRecord.confirmations()),
                depositRecord.createdAt(),
                Instant.now()
        );
    }

    @Override
    public List<DepositRecord> listDeposits(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return List.of();
    }

    private void validateRecord(DepositRecord depositRecord) {
        if (!depositRecord.amount().isPositive()) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "Deposit amount must be greater than zero");
        }
    }

    private IdempotencyKey buildIdempotencyKey(DepositRecord depositRecord) {
        return new IdempotencyKey(
                "deposit:%s:%s:%s".formatted(
                        depositRecord.asset().value(),
                        depositRecord.chain().name(),
                        depositRecord.txHash()
                )
        );
    }

    private DepositStatus resolveStubStatus(DepositStatus currentStatus, int confirmations) {
        if (currentStatus == DepositStatus.FAILED || currentStatus == DepositStatus.IGNORED) {
            return currentStatus;
        }
        return confirmations > 0 ? DepositStatus.CONFIRMING : DepositStatus.PENDING;
    }
}
