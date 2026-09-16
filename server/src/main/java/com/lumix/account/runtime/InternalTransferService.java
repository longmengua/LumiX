package com.lumix.account.runtime;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.common.RequestId;
import com.lumix.ledger.application.posting.LedgerPostingCommand;
import com.lumix.ledger.domain.LedgerBusinessReferenceType;
import com.lumix.ledger.domain.LedgerDirection;
import com.lumix.ledger.domain.LedgerEntryDraft;
import com.lumix.ledger.domain.LedgerJournalDraft;
import com.lumix.ledger.runtime.LedgerPostingActor;
import com.lumix.ledger.runtime.LedgerPostingExecutionCommand;
import com.lumix.ledger.runtime.LedgerPostingExecutionResult;
import com.lumix.ledger.runtime.TransactionalLedgerPostingService;
import com.lumix.reservation.ReservationCaptureService;
import com.lumix.reservation.ReservationHoldCommand;
import com.lumix.reservation.ReservationHoldService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 同一使用者名下帳戶間的真實劃轉 runtime。
 *
 * <p>所有副作用置於同一個 primary transaction：durable transfer key、來源 HOLD、capture、雙分錄 journal
 * 與 projection refresh。任何一步無法完成即 rollback，禁止直接更新任一餘額欄位。</p>
 */
@Service
public class InternalTransferService {
    private static final String SCOPE = "INTERNAL_TRANSFER";
    private final JdbcTemplate jdbcTemplate;
    private final ReservationHoldService holdService;
    private final ReservationCaptureService captureService;
    private final TransactionalLedgerPostingService ledgerPostingService;

    @Autowired
    public InternalTransferService(JdbcTemplate jdbcTemplate, ReservationHoldService holdService,
                                   ReservationCaptureService captureService, TransactionalLedgerPostingService ledgerPostingService) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.holdService = Objects.requireNonNull(holdService, "holdService must not be null");
        this.captureService = Objects.requireNonNull(captureService, "captureService must not be null");
        this.ledgerPostingService = Objects.requireNonNull(ledgerPostingService, "ledgerPostingService must not be null");
    }

    /** 依已認證 session 的 owner 解析帳戶，browser 永遠不可指定其他使用者的 account ID。 */
    @Transactional
    public InternalTransferResult transfer(AuthenticatedUser actor, String sourceAccountType, String destinationAccountType,
                                           String assetSymbol, BigDecimal amount, String idempotencyKey) {
        Objects.requireNonNull(actor, "actor must not be null");
        InternalTransferCommand command = buildCommand(actor, sourceAccountType, destinationAccountType, assetSymbol, amount, idempotencyKey);
        String fingerprint = fingerprint(command);
        if (!claim(command, fingerprint)) {
            return replay(command, fingerprint);
        }

        String reservationId = "transfer:" + digest(command.idempotencyKey()).substring(0, 48);
        ensureDestinationAccountAsset(command.destinationAccountId(), command.assetSymbol());
        holdService.hold(new ReservationHoldCommand(reservationId, command.sourceAccountId(), command.assetSymbol(), command.transferId(),
                command.amount(), command.requestId(), "transfer-hold:" + digest(command.idempotencyKey()).substring(0, 48), command.actorId()));
        // capture 先解除 locked；後續同 transaction 的 ledger projection 重算會把 source available 正確落在新 total。
        captureService.capture(reservationId, command.requestId(), "transfer-capture:" + digest(command.idempotencyKey()).substring(0, 48), command.actorId());
        LedgerPostingExecutionResult posted = ledgerPostingService.post(new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(new RequestId(command.requestId()), new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ADJUSTMENT, command.transferId(), List.of(
                                new LedgerEntryDraft(new AccountId(command.sourceAccountId()), new AssetSymbol(command.assetSymbol()), LedgerDirection.DEBIT, command.amount(), 1),
                                new LedgerEntryDraft(new AccountId(command.destinationAccountId()), new AssetSymbol(command.assetSymbol()), LedgerDirection.CREDIT, command.amount(), 2)
                        )), Instant.now()),
                "transfer-ledger:" + digest(command.idempotencyKey()).substring(0, 48), new LedgerPostingActor("USER", command.actorId())
        ));
        jdbcTemplate.update("INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome, reason) VALUES ('USER', ?, 'INTERNAL_TRANSFER', 'LEDGER_JOURNAL', ?, ?, 'SUCCESS', ?)",
                command.actorId(), Long.toString(posted.ledgerJournalId()), command.requestId(), command.sourceAccountId() + "->" + command.destinationAccountId() + ";asset=" + command.assetSymbol() + ";amount=" + command.amount().toPlainString());
        jdbcTemplate.update("UPDATE idempotency_keys SET status = 'COMPLETED', resource_type = 'LEDGER_JOURNAL', resource_id = ?, response_summary = ?, updated_at = CURRENT_TIMESTAMP WHERE scope = ? AND idempotency_key = ? AND status = 'IN_PROGRESS'",
                Long.toString(posted.ledgerJournalId()), fingerprint, SCOPE, command.idempotencyKey());
        return new InternalTransferResult(posted.ledgerJournalId(), false);
    }

    private InternalTransferCommand buildCommand(AuthenticatedUser actor, String fromType, String toType, String asset, BigDecimal amount, String key) {
        String normalizedAsset = Objects.requireNonNull(asset, "asset must not be null").trim().toUpperCase(Locale.ROOT);
        String source = accountId(actor.userId(), fromType);
        String destination = accountId(actor.userId(), toType);
        String transferId = "transfer:" + digest(actor.userId() + "|" + source + "|" + destination + "|" + normalizedAsset + "|" + key).substring(0, 48);
        String requestId = "transfer-" + digest(transferId).substring(0, 48);
        return new InternalTransferCommand(transferId, actor.userId(), source, destination, normalizedAsset, amount, requestId, key, actor.userId());
    }

    private String accountId(String ownerUserId, String accountType) {
        String normalizedType = Objects.requireNonNull(accountType, "accountType must not be null").trim().toUpperCase();
        List<String> accounts = jdbcTemplate.query("SELECT account_id FROM accounts WHERE user_id = ? AND account_type = ? AND status = 'ACTIVE'",
                (row, number) -> row.getString(1), ownerUserId, normalizedType);
        if (accounts.size() != 1) throw new IllegalArgumentException("eligible owner account is required");
        return accounts.getFirst();
    }

    private boolean claim(InternalTransferCommand command, String fingerprint) {
        return jdbcTemplate.update("INSERT INTO idempotency_keys (scope, idempotency_key, request_id, status, response_summary) VALUES (?, ?, ?, 'IN_PROGRESS', ?) ON CONFLICT (scope, idempotency_key) DO NOTHING",
                SCOPE, command.idempotencyKey(), command.requestId(), fingerprint) == 1;
    }

    private void ensureDestinationAccountAsset(String accountId, String assetSymbol) {
        Integer activeAsset = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assets WHERE asset_symbol = ? AND status = 'ACTIVE'", Integer.class, assetSymbol);
        if (activeAsset == null || activeAsset != 1) throw new IllegalArgumentException("asset is not active for transfer");
        jdbcTemplate.update("INSERT INTO account_assets (account_id, asset_symbol, status) VALUES (?, ?, 'ACTIVE') ON CONFLICT (account_id, asset_symbol) DO NOTHING", accountId, assetSymbol);
        Integer activeRelation = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_assets WHERE account_id = ? AND asset_symbol = ? AND status = 'ACTIVE'", Integer.class, accountId, assetSymbol);
        if (activeRelation == null || activeRelation != 1) throw new IllegalArgumentException("destination account asset is not active");
    }

    private InternalTransferResult replay(InternalTransferCommand command, String fingerprint) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT request_id, status, resource_id, response_summary FROM idempotency_keys WHERE scope = ? AND idempotency_key = ?", SCOPE, command.idempotencyKey());
        if (rows.size() != 1 || !command.requestId().equals(rows.getFirst().get("request_id")) || !fingerprint.equals(rows.getFirst().get("response_summary"))
                || !"COMPLETED".equals(rows.getFirst().get("status")) || rows.getFirst().get("resource_id") == null) {
            throw new IllegalStateException("internal transfer idempotency key is not safely replayable");
        }
        return new InternalTransferResult(Long.parseLong(rows.getFirst().get("resource_id").toString()), true);
    }

    private static String fingerprint(InternalTransferCommand command) {
        return digest(command.ownerUserId() + "|" + command.sourceAccountId() + "|" + command.destinationAccountId() + "|"
                + command.assetSymbol() + "|" + command.amount().toPlainString() + "|" + command.actorId());
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 must be available", exception); }
    }

    public record InternalTransferResult(long ledgerJournalId, boolean replayed) { }
}
