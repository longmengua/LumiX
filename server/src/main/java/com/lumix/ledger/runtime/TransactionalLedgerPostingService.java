package com.lumix.ledger.runtime;

import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerInvariantPolicy;
import com.lumix.ledger.domain.LedgerInvariantViolation;
import com.lumix.ledger.domain.LedgerJournalDraft;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 真實資料庫的 append-only ledger posting service。
 *
 * <p>這是資產 runtime 的最低層內部寫入邊界。所有可持久化的副作用都必須位於同一個 database
 * transaction：idempotency claim、帳戶／資產驗證、journal/entry append、outbox evidence、audit
 * evidence 與 completed result。任何一步失敗都回滾，絕不直接修改 {@code balance_projections}。</p>
 */
@Service
public class TransactionalLedgerPostingService {

    private static final String IDEMPOTENCY_SCOPE = "LEDGER_POSTING";
    private final JdbcTemplate jdbcTemplate;
    private final LedgerInvariantPolicy invariantPolicy;
    private final LedgerBalanceProjectionUpdater balanceProjectionUpdater;

    public TransactionalLedgerPostingService(
            JdbcTemplate jdbcTemplate,
            LedgerInvariantPolicy invariantPolicy,
            LedgerBalanceProjectionUpdater balanceProjectionUpdater
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.invariantPolicy = Objects.requireNonNull(invariantPolicy, "invariantPolicy must not be null");
        this.balanceProjectionUpdater = Objects.requireNonNull(balanceProjectionUpdater, "balanceProjectionUpdater must not be null");
    }

    /**
     * 原子地 append 已驗證的 double-entry journal。
     *
     * <p>沒有 controller 會直接呼叫此方法。上游 bounded context 必須先完成自己的 owner、風控與
     * command authorization；本服務只保護不可變帳本的資料完整性與可重送性。</p>
     */
    @Transactional
    public LedgerPostingExecutionResult post(LedgerPostingExecutionCommand executionCommand) {
        Objects.requireNonNull(executionCommand, "executionCommand must not be null");
        LedgerPostingCommand command = executionCommand.postingCommand();
        LedgerJournalDraft journal = command.journalDraft();
        String fingerprint = fingerprint(executionCommand);

        // 先 claim durable key。ON CONFLICT 不拋例外，才可在同一 transaction 安全讀回 concurrent winner。
        if (!claimIdempotencyKey(command, executionCommand.idempotencyKey(), fingerprint)) {
            return replayExistingResult(command, executionCommand.idempotencyKey(), fingerprint);
        }

        validateInvariant(journal);
        validatePostingReferences(journal);

        long journalId = appendJournal(command);
        appendEntries(journalId, journal.entries());
        balanceProjectionUpdater.refreshAffectedBalances(journal.entries());
        appendOutboxEvidence(command, journalId);
        appendAuditEvidence(command, executionCommand.actor(), journalId);
        completeIdempotencyKey(executionCommand.idempotencyKey(), fingerprint, journalId);
        return new LedgerPostingExecutionResult(journalId, false);
    }

    private boolean claimIdempotencyKey(LedgerPostingCommand command, String key, String fingerprint) {
        return jdbcTemplate.update(
                "INSERT INTO idempotency_keys (scope, idempotency_key, request_id, status, response_summary) "
                        + "VALUES (?, ?, ?, 'IN_PROGRESS', ?) ON CONFLICT (scope, idempotency_key) DO NOTHING",
                IDEMPOTENCY_SCOPE, key, command.requestId().value(), fingerprintSummary(fingerprint)
        ) == 1;
    }

    private LedgerPostingExecutionResult replayExistingResult(LedgerPostingCommand command, String key, String fingerprint) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT request_id, status, resource_id, response_summary FROM idempotency_keys "
                        + "WHERE scope = ? AND idempotency_key = ?",
                IDEMPOTENCY_SCOPE, key
        );
        if (rows.size() != 1) {
            throw new IllegalStateException("idempotency key conflict could not be resolved safely");
        }
        Map<String, Object> row = rows.getFirst();
        if (!command.requestId().value().equals(row.get("request_id"))
                || !fingerprintSummary(fingerprint).equals(row.get("response_summary"))) {
            throw new IllegalArgumentException("idempotency key cannot be reused for a different ledger posting");
        }
        if (!"COMPLETED".equals(row.get("status")) || row.get("resource_id") == null) {
            // 既有未完成 key 可能是未來人工恢復流程留下的證據，這裡絕不能猜測重試或覆寫。
            throw new IllegalStateException("existing ledger idempotency key is not safely replayable");
        }
        return new LedgerPostingExecutionResult(Long.parseLong(row.get("resource_id").toString()), true);
    }

    private void validateInvariant(LedgerJournalDraft journal) {
        List<LedgerInvariantViolation> violations = invariantPolicy.validate(journal);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("ledger journal invariant failed: "
                    + violations.stream().map(LedgerInvariantViolation::ruleCode).toList());
        }
    }

    private void validatePostingReferences(LedgerJournalDraft journal) {
        for (LedgerEntryDraft entry : journal.entries()) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT a.status AS account_status, aa.status AS account_asset_status, "
                            + "ast.status AS asset_status, ast.precision_scale "
                            + "FROM accounts a JOIN account_assets aa ON aa.account_id = a.account_id "
                            + "JOIN assets ast ON ast.asset_symbol = aa.asset_symbol "
                            + "WHERE a.account_id = ? AND aa.asset_symbol = ?",
                    entry.accountId().value(), entry.assetSymbol().value()
            );
            if (rows.size() != 1) {
                throw new IllegalArgumentException("ledger entry must reference an existing account-asset relation");
            }
            Map<String, Object> row = rows.getFirst();
            // FROZEN 是否可收款取決於後續 command policy；帳本層只禁止已關閉帳戶或停用資產被寫入。
            if ("CLOSED".equals(row.get("account_status"))
                    || !"ACTIVE".equals(row.get("account_asset_status"))
                    || !"ACTIVE".equals(row.get("asset_status"))) {
                throw new IllegalArgumentException("ledger entry references an unavailable account or asset");
            }
            int precisionScale = ((Number) row.get("precision_scale")).intValue();
            if (entry.amount().scale() > precisionScale) {
                throw new IllegalArgumentException("ledger entry amount exceeds asset precision");
            }
        }
    }


    private long appendJournal(LedgerPostingCommand command) {
        LedgerJournalDraft journal = command.journalDraft();
        Long journalId = jdbcTemplate.queryForObject(
                "INSERT INTO ledger_journals "
                        + "(business_reference_type, business_reference_id, request_id, posted_at) "
                        + "VALUES (?, ?, ?, ?) RETURNING ledger_journal_id",
                Long.class,
                journal.businessReferenceType().name(), journal.businessReferenceId(), command.requestId().value(),
                Timestamp.from(command.submittedAt())
        );
        if (journalId == null) {
            throw new IllegalStateException("ledger journal id was not returned after append");
        }
        return journalId;
    }

    private void appendEntries(long journalId, List<LedgerEntryDraft> entries) {
        for (LedgerEntryDraft entry : entries) {
            jdbcTemplate.update(
                    "INSERT INTO ledger_entries "
                            + "(ledger_journal_id, entry_sequence, account_id, asset_symbol, direction, amount) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    journalId, entry.entrySequence(), entry.accountId().value(), entry.assetSymbol().value(),
                    entry.direction().name(), entry.amount()
            );
        }
    }

    private void appendOutboxEvidence(LedgerPostingCommand command, long journalId) {
        // outbox 只留下 transactional evidence；本服務不 publisher，也不在 commit 前呼叫外部系統。
        jdbcTemplate.update(
                "INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, status, request_id) "
                        + "VALUES ('LEDGER_JOURNAL', ?, 'LEDGER_JOURNAL_POSTED', ?, 'READY', ?)",
                Long.toString(journalId), "ledgerJournalId=" + journalId, command.requestId().value()
        );
    }

    private void appendAuditEvidence(LedgerPostingCommand command, LedgerPostingActor actor, long journalId) {
        jdbcTemplate.update(
                "INSERT INTO audit_logs "
                        + "(actor_type, actor_id, action_type, target_type, target_id, request_id, outcome, reason) "
                        + "VALUES (?, ?, 'LEDGER_POSTING', 'LEDGER_JOURNAL', ?, ?, 'SUCCESS', ?)",
                actor.actorType(), actor.actorId(), Long.toString(journalId), command.requestId().value(),
                command.journalDraft().businessReferenceType().name() + ":" + command.journalDraft().businessReferenceId()
        );
    }

    private void completeIdempotencyKey(String key, String fingerprint, long journalId) {
        int updated = jdbcTemplate.update(
                "UPDATE idempotency_keys SET status = 'COMPLETED', resource_type = 'LEDGER_JOURNAL', "
                        + "resource_id = ?, response_summary = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE scope = ? AND idempotency_key = ? AND status = 'IN_PROGRESS'",
                Long.toString(journalId), fingerprintSummary(fingerprint), IDEMPOTENCY_SCOPE, key
        );
        if (updated != 1) {
            throw new IllegalStateException("ledger idempotency key could not be completed safely");
        }
    }

    static String fingerprintSummary(String fingerprint) {
        return "ledger-posting-fingerprint:" + fingerprint;
    }

    static String fingerprint(LedgerPostingExecutionCommand executionCommand) {
        LedgerPostingCommand command = executionCommand.postingCommand();
        // submittedAt 是 server 執行時間而非業務 payload；納入會讓相同 command 的安全重送被誤判為不同請求。
        StringBuilder canonical = new StringBuilder()
                .append(command.requestId().value()).append('\n')
                .append(command.journalDraft().businessReferenceType()).append('\n')
                .append(command.journalDraft().businessReferenceId()).append('\n')
                .append(executionCommand.actor().actorType()).append('\n')
                .append(executionCommand.actor().actorId()).append('\n');
        for (LedgerEntryDraft entry : command.journalDraft().entries()) {
            canonical.append(entry.entrySequence()).append('|')
                    .append(entry.accountId().value()).append('|')
                    .append(entry.assetSymbol().value()).append('|')
                    .append(entry.direction()).append('|')
                    .append(entry.amount().toPlainString()).append('\n');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available for ledger idempotency", exception);
        }
    }
}
