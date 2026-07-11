package com.lumix.ledger.application.idempotency;

import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;

import java.util.List;
import java.util.Objects;

/**
 * ledger idempotency 與 request identity 的設計政策。
 *
 * 這個 policy 只把 requestId、idempotency key 與 business reference 的角色拆清楚，
 * 不會查詢 idempotency 主檔，也不會做任何資料庫鎖定或重試控制。
 */
public final class LedgerIdempotencyDesignPolicy {

    /**
     * 將 posting command 轉成 request identity contract。
     *
     * requestId 只代表追蹤與關聯；idempotency key 才是未來 duplicate prevention contract 的核心。
     */
    public LedgerRequestIdentityContract describeRequestIdentity(LedgerPostingCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");

        LedgerBusinessReferenceType businessReferenceType = command.journalDraft().businessReferenceType();
        String businessReferenceId = command.journalDraft().businessReferenceId();
        return new LedgerRequestIdentityContract(
                command.requestId(),
                LedgerIdempotencyScope.LEDGER_POSTING,
                idempotencyKey,
                businessReferenceType,
                businessReferenceId
        );
    }

    /**
     * 將 posting command 與決策狀態包成設計輸出。
     *
     * 這只是在設計層面整理契約，不代表已完成 idempotency lookup、lock 或 replay。
     */
    public LedgerIdempotencyDesign describe(
            LedgerPostingCommand command,
            String idempotencyKey,
            LedgerIdempotencyDecision decision
    ) {
        Objects.requireNonNull(decision, "decision must not be null");

        LedgerRequestIdentityContract requestIdentity = describeRequestIdentity(command, idempotencyKey);
        return new LedgerIdempotencyDesign(
                requestIdentity,
                decision,
                List.of(
                        "requestId 只用於 trace / correlation / audit linkage",
                        "idempotency key 才負責 duplicate prevention contract",
                        "business reference 只能識別業務來源，不能單獨取代 idempotency key",
                        "未來 ledger append runtime 必須先檢查 idempotency，再進入 journal / entry append",
                        "所有正式 idempotency runtime 都屬於 HUMAN_REVIEW_REQUIRED"
                )
        );
    }
}
