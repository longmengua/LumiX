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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 使用者間平台內部轉帳 runtime。
 *
 * <p>此流程只處理 LumiX 內兩個已啟用現貨帳戶之間的 immutable ledger movement；不觸及鏈上地址、provider、私鑰或
 * 外部資金通道。HUMAN_REVIEW_REQUIRED：任何 reservation、ledger 或 idempotency 規則調整都必須由人工審核。</p>
 */
@Service
public class PlatformInternalTransferService {
    private static final String SCOPE = "PLATFORM_INTERNAL_TRANSFER";
    private static final String SPOT = "SPOT";
    private final JdbcTemplate jdbcTemplate;
    private final ReservationHoldService holdService;
    private final ReservationCaptureService captureService;
    private final TransactionalLedgerPostingService ledgerPostingService;

    public PlatformInternalTransferService(JdbcTemplate jdbcTemplate, ReservationHoldService holdService,
                                           ReservationCaptureService captureService, TransactionalLedgerPostingService ledgerPostingService) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.holdService = Objects.requireNonNull(holdService, "holdService must not be null");
        this.captureService = Objects.requireNonNull(captureService, "captureService must not be null");
        this.ledgerPostingService = Objects.requireNonNull(ledgerPostingService, "ledgerPostingService must not be null");
    }

    /**
     * 從 session owner 的現貨帳戶將資產轉入收款 UUID 的現貨帳戶。
     *
     * <p>claim、hold、capture、雙分錄、投影重建、audit 與完成標記處於單一 transaction；任一步失敗時資料庫回滾，
     * 因而不會出現只扣款或只入帳的中間狀態。</p>
     */
    @Transactional
    public PlatformInternalTransferResult transfer(AuthenticatedUser actor, String recipientUserId, String assetSymbol,
                                                    BigDecimal amount, String idempotencyKey) {
        Objects.requireNonNull(actor, "actor must not be null");
        Command command = buildCommand(actor, recipientUserId, assetSymbol, amount, idempotencyKey);
        String fingerprint = fingerprint(command);
        if (!claim(command, fingerprint)) {
            return replay(command, fingerprint);
        }

        ensureSourceAccountAsset(command.sourceAccountId(), command.assetSymbol());
        ensureDestinationAccountAsset(command.destinationAccountId(), command.assetSymbol());
        String reservationId = "platform-transfer:" + digest(command.idempotencyKey()).substring(0, 44);
        holdService.hold(new ReservationHoldCommand(reservationId, command.sourceAccountId(), command.assetSymbol(), command.transferId(),
                command.amount(), command.requestId(), "platform-transfer-hold:" + digest(command.idempotencyKey()).substring(0, 40), command.actorId()));
        // capture 與 journal posting 同在 primary transaction，避免 hold 已建立但資產沒有完成扣取。
        captureService.capture(reservationId, command.requestId(), "platform-transfer-capture:" + digest(command.idempotencyKey()).substring(0, 36), command.actorId());
        LedgerPostingExecutionResult posted = ledgerPostingService.post(new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(new RequestId(command.requestId()), new LedgerJournalDraft(
                        LedgerBusinessReferenceType.ADJUSTMENT, command.transferId(), List.of(
                                new LedgerEntryDraft(new AccountId(command.sourceAccountId()), new AssetSymbol(command.assetSymbol()), LedgerDirection.DEBIT, command.amount(), 1),
                                new LedgerEntryDraft(new AccountId(command.destinationAccountId()), new AssetSymbol(command.assetSymbol()), LedgerDirection.CREDIT, command.amount(), 2)
                        )), Instant.now()),
                "platform-transfer-ledger:" + digest(command.idempotencyKey()).substring(0, 36), new LedgerPostingActor("USER", command.actorId())
        ));
        jdbcTemplate.update("INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome, reason) VALUES ('USER', ?, 'PLATFORM_INTERNAL_TRANSFER', 'LEDGER_JOURNAL', ?, ?, 'SUCCESS', ?)",
                command.actorId(), Long.toString(posted.ledgerJournalId()), command.requestId(),
                command.sourceAccountId() + "->" + command.destinationAccountId() + ";asset=" + command.assetSymbol() + ";amount=" + command.amount().toPlainString());
        jdbcTemplate.update("UPDATE idempotency_keys SET status = 'COMPLETED', resource_type = 'LEDGER_JOURNAL', resource_id = ?, response_summary = ?, updated_at = CURRENT_TIMESTAMP WHERE scope = ? AND idempotency_key = ? AND status = 'IN_PROGRESS'",
                Long.toString(posted.ledgerJournalId()), fingerprint, SCOPE, command.idempotencyKey());
        return new PlatformInternalTransferResult(posted.ledgerJournalId(), false);
    }

    private Command buildCommand(AuthenticatedUser actor, String recipientUserId, String assetSymbol, BigDecimal amount, String idempotencyKey) {
        String recipient = required(recipientUserId, "recipient user ID");
        if (recipient.length() > 64 || actor.userId().equals(recipient)) {
            throw new IllegalArgumentException("recipient user is invalid");
        }
        String asset = required(assetSymbol, "asset symbol").toUpperCase(Locale.ROOT);
        String key = required(idempotencyKey, "idempotency key");
        String source = userSpotAccountId(actor.userId());
        String destination = userSpotAccountId(recipient);
        String transferId = "platform-transfer:" + digest(actor.userId() + "|" + recipient + "|" + asset + "|" + key).substring(0, 40);
        String requestId = "platform-transfer-" + digest(transferId).substring(0, 36);
        return new Command(transferId, source, destination, asset, amount, requestId, key, actor.userId(), recipient);
    }

    private String userSpotAccountId(String userId) {
        List<String> accounts = jdbcTemplate.query("SELECT account.account_id FROM accounts account JOIN users user_row ON user_row.user_id = account.user_id WHERE account.user_id = ? AND account.account_type = ? AND account.status = 'ACTIVE' AND account.account_category = 'USER' AND user_row.status = 'ACTIVE'",
                (row, number) -> row.getString(1), userId, SPOT);
        if (accounts.size() != 1) {
            throw new IllegalArgumentException("eligible spot account is required");
        }
        return accounts.getFirst();
    }

    private void ensureSourceAccountAsset(String accountId, String assetSymbol) {
        Integer activeRelation = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_assets WHERE account_id = ? AND asset_symbol = ? AND status = 'ACTIVE'", Integer.class, accountId, assetSymbol);
        if (activeRelation == null || activeRelation != 1) {
            throw new IllegalArgumentException("source account asset is not active");
        }
    }

    private void ensureDestinationAccountAsset(String accountId, String assetSymbol) {
        Integer activeAsset = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assets WHERE asset_symbol = ? AND status = 'ACTIVE'", Integer.class, assetSymbol);
        if (activeAsset == null || activeAsset != 1) {
            throw new IllegalArgumentException("asset is not active for transfer");
        }
        jdbcTemplate.update("INSERT INTO account_assets (account_id, asset_symbol, status) VALUES (?, ?, 'ACTIVE') ON CONFLICT (account_id, asset_symbol) DO NOTHING", accountId, assetSymbol);
        Integer activeRelation = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_assets WHERE account_id = ? AND asset_symbol = ? AND status = 'ACTIVE'", Integer.class, accountId, assetSymbol);
        if (activeRelation == null || activeRelation != 1) {
            throw new IllegalArgumentException("destination account asset is not active");
        }
    }

    private boolean claim(Command command, String fingerprint) {
        return jdbcTemplate.update("INSERT INTO idempotency_keys (scope, idempotency_key, request_id, status, response_summary) VALUES (?, ?, ?, 'IN_PROGRESS', ?) ON CONFLICT (scope, idempotency_key) DO NOTHING",
                SCOPE, command.idempotencyKey(), command.requestId(), fingerprint) == 1;
    }

    private PlatformInternalTransferResult replay(Command command, String fingerprint) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT request_id, status, resource_id, response_summary FROM idempotency_keys WHERE scope = ? AND idempotency_key = ?", SCOPE, command.idempotencyKey());
        if (rows.size() != 1 || !command.requestId().equals(rows.getFirst().get("request_id")) || !fingerprint.equals(rows.getFirst().get("response_summary"))
                || !"COMPLETED".equals(rows.getFirst().get("status")) || rows.getFirst().get("resource_id") == null) {
            throw new IllegalStateException("platform internal transfer idempotency key is not safely replayable");
        }
        return new PlatformInternalTransferResult(Long.parseLong(rows.getFirst().get("resource_id").toString()), true);
    }

    private static String fingerprint(Command command) {
        return digest(command.actorId() + "|" + command.recipientUserId() + "|" + command.sourceAccountId() + "|" + command.destinationAccountId() + "|"
                + command.assetSymbol() + "|" + command.amount().toPlainString());
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private record Command(String transferId, String sourceAccountId, String destinationAccountId, String assetSymbol,
                           BigDecimal amount, String requestId, String idempotencyKey, String actorId, String recipientUserId) {
        private Command {
            Objects.requireNonNull(amount, "amount must not be null");
            if (amount.signum() <= 0 || sourceAccountId.equals(destinationAccountId)) {
                throw new IllegalArgumentException("platform internal transfer command is invalid");
            }
        }
    }

    public record PlatformInternalTransferResult(long ledgerJournalId, boolean replayed) { }
}
