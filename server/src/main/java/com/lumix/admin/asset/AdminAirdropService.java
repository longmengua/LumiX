package com.lumix.admin.asset;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.admin.superadmin.SuperAdminAccessService;
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
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 受治理的管理端空投入帳服務。
 *
 * <p>此服務只能由已驗證的 super-admin 呼叫。它不直接寫入餘額：使用者 CREDIT 與交易所空投帳戶 DEBIT
 * 一律透過 immutable ledger posting 成對追加，再由 ledger runtime 重建 projection。</p>
 */
@Service
public class AdminAirdropService {

    private static final String EXCHANGE_AIRDROP_ACCOUNT_ID = "system:airdrop:spot";
    private static final String AIRDROP_DESTINATION_ACCOUNT_TYPE = "SPOT";
    private final SuperAdminAccessService access;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionalLedgerPostingService ledgerPostingService;
    private final Clock clock;

    @Autowired
    public AdminAirdropService(
            SuperAdminAccessService access,
            JdbcTemplate jdbcTemplate,
            TransactionalLedgerPostingService ledgerPostingService
    ) {
        this(access, jdbcTemplate, ledgerPostingService, Clock.systemUTC());
    }

    AdminAirdropService(
            SuperAdminAccessService access,
            JdbcTemplate jdbcTemplate,
            TransactionalLedgerPostingService ledgerPostingService,
            Clock clock
    ) {
        this.access = Objects.requireNonNull(access, "access must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.ledgerPostingService = Objects.requireNonNull(ledgerPostingService, "ledgerPostingService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 以活動／使用者／帳戶／資產的固定去重鍵執行一次空投。
     *
     * <p>相同活動的安全重送會讀回同一筆 ledger journal；任一欄位不同則 fail closed，避免 UI 重送或
     * 操作者誤用活動識別碼造成第二次資金發放。</p>
     */
    @Transactional
    public AdminAirdropResult grant(AuthenticatedUser actor, AdminAirdropCommand command) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        Objects.requireNonNull(command, "command must not be null");

        String targetAccountId = resolveTargetAccount(command);
        ensureActiveAccountAsset(targetAccountId, command.assetSymbol());
        ensureActiveAccountAsset(EXCHANGE_AIRDROP_ACCOUNT_ID, command.assetSymbol());

        String businessReferenceId = airdropReference(command);
        String idempotencyKey = "airdrop:" + digest(businessReferenceId);
        LedgerJournalDraft journal = new LedgerJournalDraft(
                LedgerBusinessReferenceType.ADJUSTMENT,
                businessReferenceId,
                List.of(
                        new LedgerEntryDraft(new AccountId(EXCHANGE_AIRDROP_ACCOUNT_ID), new AssetSymbol(command.assetSymbol()),
                                LedgerDirection.DEBIT, command.amount(), 1L),
                        new LedgerEntryDraft(new AccountId(targetAccountId), new AssetSymbol(command.assetSymbol()),
                                LedgerDirection.CREDIT, command.amount(), 2L)
                )
        );
        LedgerPostingExecutionResult posting = ledgerPostingService.post(new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(new RequestId("airdrop-" + digest(businessReferenceId).substring(0, 48)), journal, Instant.now(clock)),
                idempotencyKey,
                new LedgerPostingActor("ADMIN", actor.userId())
        ));

        // ledger 層已記錄 journal evidence；此筆補上活動、原因與目標使用者，供管理端稽核而不曝露到一般用戶。
        if (!posting.replayed()) {
            jdbcTemplate.update(
                    "INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome, reason) "
                            + "VALUES ('ADMIN', ?, 'ADMIN_AIRDROP', 'USER_ACCOUNT', ?, ?, 'SUCCESS', ?)",
                    actor.userId(), targetAccountId, "airdrop-" + digest(businessReferenceId).substring(0, 48),
                    "activity=" + command.activityId() + ";asset=" + command.assetSymbol() + ";amount="
                            + command.amount().toPlainString() + ";reason=" + command.reason()
            );
        }
        return new AdminAirdropResult(posting.ledgerJournalId(), posting.replayed());
    }

    private String resolveTargetAccount(AdminAirdropCommand command) {
        List<String> accountIds = jdbcTemplate.query(
                "SELECT account_id FROM accounts WHERE user_id = ? AND account_type = ? AND status IN ('ACTIVE', 'FROZEN')",
                (resultSet, rowNumber) -> resultSet.getString(1), command.targetUserId(), AIRDROP_DESTINATION_ACCOUNT_TYPE
        );
        if (accountIds.size() != 1) {
            throw new IllegalArgumentException("target user does not have an eligible account");
        }
        return accountIds.getFirst();
    }

    private void ensureActiveAccountAsset(String accountId, String assetSymbol) {
        List<Map<String, Object>> assets = jdbcTemplate.queryForList(
                "SELECT status FROM assets WHERE asset_symbol = ?", assetSymbol
        );
        if (assets.size() != 1 || !"ACTIVE".equals(assets.getFirst().get("status"))) {
            throw new IllegalArgumentException("asset is not active for airdrop");
        }
        // account_assets 是 ledger entry 的前置關聯；只在 active asset 下建立，避免以空投繞過資產啟用流程。
        jdbcTemplate.update(
                "INSERT INTO account_assets (account_id, asset_symbol, status) VALUES (?, ?, 'ACTIVE') "
                        + "ON CONFLICT (account_id, asset_symbol) DO NOTHING",
                accountId, assetSymbol
        );
        Integer active = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_assets WHERE account_id = ? AND asset_symbol = ? AND status = 'ACTIVE'",
                Integer.class, accountId, assetSymbol
        );
        if (active == null || active != 1) {
            throw new IllegalArgumentException("account asset is not active for airdrop");
        }
    }

    private static String airdropReference(AdminAirdropCommand command) {
        return "airdrop:" + command.activityId() + ":" + command.targetUserId() + ":"
                + AIRDROP_DESTINATION_ACCOUNT_TYPE + ":" + command.assetSymbol();
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
