package com.lumix.ledger.application.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 ledger idempotency 與 request identity 只停留在設計契約，不會被誤升級成 runtime。
 */
class LedgerIdempotencyDesignPolicyTest {

    private final LedgerIdempotencyDesignPolicy policy = new LedgerIdempotencyDesignPolicy();

    /**
     * 確認 requestId、idempotency key 與 business reference 是三個不同語意。
     *
     * 這個 case 必須存在，因為 requestId 只做 trace / correlation，不代表 duplicate prevention 已完成。
     */
    @Test
    void requestIdentitySeparatesTraceCorrelationAndDuplicatePrevention() {
        LedgerPostingCommand command = createCommand();

        LedgerRequestIdentityContract requestIdentity = policy.describeRequestIdentity(command, "ledger-posting-idem-001");

        assertEquals(new RequestId("req-idem-001"), requestIdentity.requestId());
        assertEquals(LedgerIdempotencyScope.LEDGER_POSTING, requestIdentity.scope());
        assertEquals("ledger-posting-idem-001", requestIdentity.idempotencyKey());
        assertEquals(LedgerBusinessReferenceType.ORDER, requestIdentity.businessReferenceType());
        assertEquals("order-idem-001", requestIdentity.businessReferenceId());
        assertNotEquals(requestIdentity.requestId().value(), requestIdentity.idempotencyKey());
    }

    /**
     * 確認 decision contract 可以表達所有預期狀態。
     *
     * 這個 case 必須存在，因為未來 runtime 需要能區分 new request、duplicate、in progress 與 conflict。
     */
    @Test
    void decisionContractSupportsAllExpectedStates() {
        LedgerPostingCommand command = createCommand();

        for (LedgerIdempotencyDecision decision : LedgerIdempotencyDecision.values()) {
            LedgerIdempotencyDesign design = policy.describe(command, "ledger-posting-idem-002", decision);
            assertEquals(decision, design.decision());
            assertEquals(LedgerIdempotencyScope.LEDGER_POSTING, design.requestIdentity().scope());
            assertEquals(new RequestId("req-idem-001"), design.requestIdentity().requestId());
            assertFalse(design.notes().isEmpty());
            assertTrue(design.notes().stream().anyMatch(note -> note.contains("requestId")));
        }
    }

    /**
     * 確認 business reference 不能單獨取代 idempotency key。
     *
     * 這個 case 必須存在，因為如果 key 可以缺席，就會把業務來源誤當成 duplicate prevention 保證。
     */
    @Test
    void blankIdempotencyKeyIsRejected() {
        LedgerPostingCommand command = createCommand();

        assertThrows(IllegalArgumentException.class, () -> policy.describeRequestIdentity(command, "   "));
    }

    private static LedgerPostingCommand createCommand() {
        return new LedgerPostingCommand(
                new RequestId("req-idem-001"),
                new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ORDER,
                        "order-idem-001",
                        List.of(
                                new LedgerEntryDraft(new AccountId("acct-a"), new AssetSymbol("USDT"), LedgerDirection.DEBIT,
                                        new BigDecimal("15"), 1L),
                                new LedgerEntryDraft(new AccountId("acct-b"), new AssetSymbol("USDT"), LedgerDirection.CREDIT,
                                        new BigDecimal("15"), 2L)
                        )
                ),
                Instant.parse("2026-07-08T14:00:00Z")
        );
    }
}
