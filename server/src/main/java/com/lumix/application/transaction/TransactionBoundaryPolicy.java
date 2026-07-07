package com.lumix.application.transaction;

/**
 * transaction boundary policy。
 *
 * 這個 policy 只做分類與風險標記，不開啟任何實際 transaction，也不碰資料庫 client。
 */
public class TransactionBoundaryPolicy {

    /**
     * 判定 use case 應使用 read-only 或 write transaction。
     *
     * read-only query 必須明確分開，避免後續把查詢混進寫入交易。
     */
    public TransactionBoundaryKind classify(TransactionUseCase useCase) {
        if (useCase == null) {
            return TransactionBoundaryKind.WRITE;
        }

        return switch (useCase) {
            case READ_ONLY_QUERY -> TransactionBoundaryKind.READ_ONLY;
            case STATE_MUTATION,
                 LEDGER_POSTING,
                 RESERVATION_HOLD_RELEASE,
                 WITHDRAWAL_REQUEST,
                 ORDER_PLACEMENT,
                 SETTLEMENT,
                 OUTBOX_APPEND,
                 IDEMPOTENCY_RECORD,
                 AUDIT_APPEND -> TransactionBoundaryKind.WRITE;
        };
    }

    /**
     * 判定是否屬於需要 HUMAN_REVIEW_REQUIRED 的高風險 transaction。
     *
     * 這類 use case 包含 ledger、reservation、withdrawal、order 與 settlement，
     * 不能被當成一般 write transaction 來簡化。
     */
    public boolean requiresHumanReview(TransactionUseCase useCase) {
        if (useCase == null) {
            return false;
        }

        return switch (useCase) {
            case LEDGER_POSTING,
                 RESERVATION_HOLD_RELEASE,
                 WITHDRAWAL_REQUEST,
                 ORDER_PLACEMENT,
                 SETTLEMENT -> true;
            case READ_ONLY_QUERY,
                 STATE_MUTATION,
                 OUTBOX_APPEND,
                 IDEMPOTENCY_RECORD,
                 AUDIT_APPEND -> false;
        };
    }
}
