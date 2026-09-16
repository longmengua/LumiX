package com.lumix.ledger.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.domain.LedgerJournalDraft;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** 驗證真實 ledger posting 的 idempotency 與不可變 evidence 邊界。 */
class TransactionalLedgerPostingServiceTest {

    /**
     * 成功 case 必須 append journal/entries/outbox/audit 並完成 idempotency；這保護資金事件不能只寫其中一張表。
     */
    @Test
    void postAppendsAllDurableEvidenceForValidJournal() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(availableReference()));
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Object[].class))).thenAnswer(invocation -> {
            Class<?> requiredType = invocation.getArgument(1);
            if (requiredType == Long.class) return 91L;
            if (requiredType == BigDecimal.class) return new BigDecimal("10.00");
            return "USER";
        });
        TransactionalLedgerPostingService service = new TransactionalLedgerPostingService(
                jdbcTemplate, new LedgerInvariantPolicy(), new LedgerBalanceProjectionUpdater(jdbcTemplate));

        LedgerPostingExecutionResult result = service.post(command("idem-ledger-001"));

        assertEquals(91L, result.ledgerJournalId());
        assertFalse(result.replayed());
        String executedSql = mockingDetails(jdbcTemplate).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("update")
                        || invocation.getMethod().getName().equals("queryForObject"))
                .map(invocation -> invocation.getArgument(0, String.class))
                .collect(Collectors.joining("\n"));
        assertTrue(executedSql.contains("INSERT INTO ledger_journals"));
        assertTrue(executedSql.contains("INSERT INTO ledger_entries"));
        assertTrue(executedSql.contains("INSERT INTO outbox_events"));
        assertTrue(executedSql.contains("INSERT INTO audit_logs"));
        assertTrue(executedSql.contains("UPDATE idempotency_keys"));
    }

    /**
     * 相同 key 與相同不可變內容必須只回放既有 journal，避免重送請求產生第二組 double-entry。
     */
    @Test
    void postReplaysCompletedIdempotencyKeyWithoutAppendingAgain() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);
        LedgerPostingExecutionCommand command = command("idem-ledger-002");
        String fingerprint = TransactionalLedgerPostingService.fingerprintSummary(
                TransactionalLedgerPostingService.fingerprint(command)
        );
        // replay evidence 使用 production fingerprint helper，避免測試另寫一套 hash 演算法而掩蓋 request 差異。
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of(
                "request_id", "req-ledger-001",
                "status", "COMPLETED",
                "resource_id", "73",
                "response_summary", fingerprint
        )));
        TransactionalLedgerPostingService service = new TransactionalLedgerPostingService(
                jdbcTemplate, new LedgerInvariantPolicy(), new LedgerBalanceProjectionUpdater(jdbcTemplate));

        LedgerPostingExecutionResult result = service.post(command);

        assertEquals(73L, result.ledgerJournalId());
        assertTrue(result.replayed());
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class), any(Object[].class));
    }

    private static LedgerPostingExecutionCommand command(String idempotencyKey) {
        LedgerJournalDraft journal = new LedgerJournalDraft(
                LedgerBusinessReferenceType.ADJUSTMENT,
                "asset-runtime-foundation-001",
                List.of(
                        new LedgerEntryDraft(new AccountId("account-debit"), new AssetSymbol("USDT"),
                                LedgerDirection.DEBIT, new BigDecimal("10.00"), 1),
                        new LedgerEntryDraft(new AccountId("account-credit"), new AssetSymbol("USDT"),
                                LedgerDirection.CREDIT, new BigDecimal("10.00"), 2)
                )
        );
        return new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(new RequestId("req-ledger-001"), journal, Instant.parse("2026-09-15T00:00:00Z")),
                idempotencyKey,
                new LedgerPostingActor("SYSTEM", "asset-runtime")
        );
    }

    private static Map<String, Object> availableReference() {
        return Map.of(
                "account_status", "ACTIVE",
                "account_asset_status", "ACTIVE",
                "asset_status", "ACTIVE",
                "precision_scale", 18
        );
    }
}
