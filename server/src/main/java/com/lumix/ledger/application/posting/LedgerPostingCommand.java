package com.lumix.ledger.application.posting;

import com.lumix.common.RequestId;
import com.lumix.ledger.domain.LedgerJournalDraft;

import java.time.Instant;
import java.util.Objects;

/**
 * ledger posting application command。
 *
 * 這個 command 只封裝 requestId 與 journal draft，並保留提交時間供 plan 建立時使用；
 * 它不代表任何寫入已完成。
 */
public record LedgerPostingCommand(
        RequestId requestId,
        LedgerJournalDraft journalDraft,
        Instant submittedAt
) {

    public LedgerPostingCommand {
        // command 必須可追蹤且可稽核，否則後續的 gate 結果無法對回原始請求。
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(journalDraft, "journalDraft must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
    }
}
