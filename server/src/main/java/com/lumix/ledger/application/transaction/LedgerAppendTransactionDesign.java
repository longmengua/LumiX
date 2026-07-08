package com.lumix.ledger.application.transaction;

import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingPlan;

import java.util.List;
import java.util.Objects;

/**
 * ledger append transaction 的設計描述。
 *
 * 這份設計只說明 steps 與安全限制，不會執行任何資料庫操作。
 */
public record LedgerAppendTransactionDesign(
        RequestId requestId,
        LedgerPostingPlan postingPlan,
        List<LedgerAppendTransactionStep> steps,
        List<String> safetyNotes
) {

    public LedgerAppendTransactionDesign {
        // 設計輸出必須可重建、可審核，不能留下可變集合參考。
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(postingPlan, "postingPlan must not be null");
        Objects.requireNonNull(steps, "steps must not be null");
        Objects.requireNonNull(safetyNotes, "safetyNotes must not be null");
        steps = List.copyOf(steps);
        safetyNotes = List.copyOf(safetyNotes);
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
    }
}
