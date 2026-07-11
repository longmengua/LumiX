package com.lumix.ledger.application.posting.runtime;

import com.lumix.ledger.application.LedgerRuntimeBoundary;
import com.lumix.ledger.application.LedgerRuntimePrerequisite;
import com.lumix.ledger.application.LedgerRuntimePrerequisiteReport;
import com.lumix.ledger.application.idempotency.LedgerIdempotencyDesignPolicy;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.application.posting.LedgerPostingCommandBoundary;
import com.lumix.ledger.application.posting.LedgerPostingCommandResult;
import com.lumix.ledger.application.posting.LedgerPostingDecision;
import com.lumix.ledger.application.posting.LedgerPostingRejection;
import com.lumix.ledger.persistence.LedgerAppendOnlyPersistenceMapping;
import com.lumix.ledger.persistence.LedgerJournalPersistenceMapping;
import com.lumix.ledger.persistence.adapter.LedgerAppendOnlyJdbcAdapter;

import java.util.Objects;
import java.util.Set;

/**
 * 受控 ledger posting runtime 接線門檻。
 *
 * 這個 gate 只負責把 accepted posting plan 送進 append adapter，且前面一定要先過 request identity
 * 設計檢查與 runtime prerequisite gate；它不處理 balance projection、reservation、settlement、
 * idempotency runtime、outbox 或 audit。
 */
public final class LedgerPostingRuntimeGate {

    private final LedgerRuntimeBoundary runtimeBoundary;
    private final LedgerPostingCommandBoundary commandBoundary;
    private final LedgerAppendOnlyJdbcAdapter appendOnlyJdbcAdapter;
    private final LedgerIdempotencyDesignPolicy idempotencyDesignPolicy;

    /**
     * 建立受控 runtime gate。
     *
     * 這裡只接 application boundary 與 append adapter，不接任何 repository、transaction 或 balance mutation 服務。
     */
    public LedgerPostingRuntimeGate(
            LedgerRuntimeBoundary runtimeBoundary,
            LedgerPostingCommandBoundary commandBoundary,
            LedgerAppendOnlyJdbcAdapter appendOnlyJdbcAdapter,
            LedgerIdempotencyDesignPolicy idempotencyDesignPolicy
    ) {
        // 這個 gate 只做 controlled wiring，不能偷偷升級成完整交易 runtime。
        this.runtimeBoundary = Objects.requireNonNull(runtimeBoundary, "runtimeBoundary must not be null");
        this.commandBoundary = Objects.requireNonNull(commandBoundary, "commandBoundary must not be null");
        this.appendOnlyJdbcAdapter = Objects.requireNonNull(appendOnlyJdbcAdapter, "appendOnlyJdbcAdapter must not be null");
        this.idempotencyDesignPolicy = Objects.requireNonNull(idempotencyDesignPolicy, "idempotencyDesignPolicy must not be null");
    }

    /**
     * 受控地執行 ledger append。
     *
     * 接線順序必須是 request identity / idempotency 設計檢查、runtime prerequisite gate、command boundary、
     * 再把 accepted plan append 到 ledger。任何 rejected case 都必須在 append 之前停止。
     */
    public LedgerPostingAppendResult append(
            LedgerPostingCommand command,
            Set<LedgerRuntimePrerequisite> availablePrerequisites,
            String idempotencyKey
    ) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(availablePrerequisites, "availablePrerequisites must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");

        LedgerPostingCommandResult rejectedIdentity = validateRequestIdentity(command, idempotencyKey);
        if (rejectedIdentity != null) {
            return LedgerPostingAppendResult.rejected(rejectedIdentity);
        }

        LedgerRuntimePrerequisiteReport prerequisiteReport = runtimeBoundary.verifyPrerequisites(availablePrerequisites);
        if (!prerequisiteReport.isReady()) {
            return LedgerPostingAppendResult.rejected(LedgerPostingCommandResult.rejected(new LedgerPostingRejection(
                    "RUNTIME_PREREQUISITE_GATE_FAILED",
                    "Ledger runtime prerequisites are not ready.",
                    prerequisiteReport.missing().stream().map(LedgerRuntimePrerequisite::name).toList()
            )));
        }

        LedgerPostingCommandResult commandResult = commandBoundary.evaluate(command, availablePrerequisites);
        if (commandResult.decision() == LedgerPostingDecision.REJECTED) {
            return LedgerPostingAppendResult.rejected(commandResult);
        }

        LedgerAppendOnlyPersistenceMapping appendOnlyMapping = withRequestId(command, commandResult);
        long ledgerJournalId = appendOnlyJdbcAdapter.append(appendOnlyMapping);
        return LedgerPostingAppendResult.appended(commandResult, ledgerJournalId);
    }

    /**
     * 先做 request identity / idempotency 的設計檢查。
     *
     * 這裡只檢查設計契約，沒有 idempotency lookup、lock、retry 或 store 讀寫。
     */
    private LedgerPostingCommandResult validateRequestIdentity(LedgerPostingCommand command, String idempotencyKey) {
        try {
            idempotencyDesignPolicy.describeRequestIdentity(command, idempotencyKey);
            return null;
        } catch (RuntimeException ex) {
            return LedgerPostingCommandResult.rejected(new LedgerPostingRejection(
                    "REQUEST_IDENTITY_GATE_FAILED",
                    "Ledger request identity is not ready.",
                    java.util.List.of(
                            "requestId 只做 trace / correlation / audit linkage",
                            "idempotency key 才負責 duplicate prevention contract",
                            "business reference 不能單獨取代 idempotency key"
                    )
            ));
        }
    }

    /**
     * 把 requestId 落到 journal header，讓這次受控接線具備追蹤性。
     *
     * 這裡只補 trace / correlation 欄位，不會把 requestId 誤解成 idempotency guarantee。
     * 如果 mapping 已經帶有不同的 requestId，就必須直接失敗，避免把 trace / audit 語意靜默改寫。
     */
    private LedgerAppendOnlyPersistenceMapping withRequestId(
            LedgerPostingCommand command,
            LedgerPostingCommandResult commandResult
    ) {
        LedgerAppendOnlyPersistenceMapping mapping = commandResult.plan()
                .orElseThrow(() -> new IllegalStateException("accepted plan must exist when accepted"))
                .appendOnlyMapping();
        LedgerJournalPersistenceMapping journal = mapping.journal();
        String commandRequestId = command.requestId().value();
        String mappingRequestId = journal.requestId();
        if (mappingRequestId != null && !mappingRequestId.equals(commandRequestId)) {
            throw new IllegalStateException("Ledger journal requestId conflicts with command requestId.");
        }

        LedgerJournalPersistenceMapping traceableJournal = new LedgerJournalPersistenceMapping(
                journal.businessReferenceType(),
                journal.businessReferenceId(),
                mappingRequestId == null ? commandRequestId : mappingRequestId,
                journal.journalNote(),
                journal.postedAt()
        );
        return new LedgerAppendOnlyPersistenceMapping(traceableJournal, mapping.entries());
    }
}
