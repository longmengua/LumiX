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
 * 受治理的管理端資產調整入帳服務。
 *
 * <p>類別與 endpoint 沿用既有 airdrop 名稱以維持相容性；實際行為由 {@code activityId} 類型分流。
 * 此服務只能由已驗證的 super-admin 呼叫，且不直接寫入餘額：所有加扣款都透過 immutable ledger posting
 * 成對追加，再由 ledger runtime 重建 projection。</p>
 */
@Service
public class AdminAirdropService {

    private static final String EXCHANGE_AIRDROP_ACCOUNT_ID = "system:airdrop:spot";
    private static final String EXCHANGE_AIRDROP_USER_ID = "system:airdrop";
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
     * 以類型／使用者／帳戶／資產的固定去重鍵執行一次資產調整。
     *
     * <p>相同活動的安全重送會讀回同一筆 ledger journal；任一欄位不同則 fail closed，避免 UI 重送或
     * 操作者誤用類型造成第二次資金調整。</p>
     */
    @Transactional
    public AdminAirdropResult grant(AuthenticatedUser actor, AdminAirdropCommand command) {
        access.requireActiveSuperAdmin(Objects.requireNonNull(actor, "actor must not be null"));
        Objects.requireNonNull(command, "command must not be null");

        String targetAccountId = resolveTargetAccount(command);
        ensureActiveAccountAsset(targetAccountId, command.assetSymbol());
        ensureDesignatedAirdropFundingAccount();
        ensureActiveAccountAsset(EXCHANGE_AIRDROP_ACCOUNT_ID, command.assetSymbol());
        String businessReferenceId = adjustmentReference(command);
        String idempotencyKey = "asset-adjustment:" + digest(businessReferenceId);
        // AIRDROP 已不再共用 signed adjustment；方向是固定的 funding debit -> user credit。
        java.math.BigDecimal amount = command.amount();
        List<LedgerEntryDraft> entries = List.of(
                new LedgerEntryDraft(new AccountId(EXCHANGE_AIRDROP_ACCOUNT_ID), new AssetSymbol(command.assetSymbol()), LedgerDirection.DEBIT, amount, 1L),
                new LedgerEntryDraft(new AccountId(targetAccountId), new AssetSymbol(command.assetSymbol()), LedgerDirection.CREDIT, amount, 2L)
        );
        LedgerJournalDraft journal = new LedgerJournalDraft(
                LedgerBusinessReferenceType.ADJUSTMENT,
                businessReferenceId,
                entries
        );
        LedgerPostingExecutionResult posting = ledgerPostingService.post(new LedgerPostingExecutionCommand(
                new LedgerPostingCommand(new RequestId("asset-adjustment-" + digest(businessReferenceId).substring(0, 40)), journal, Instant.now(clock)),
                idempotencyKey,
                new LedgerPostingActor("ADMIN", actor.userId())
        ));

        // ledger 層已記錄 journal evidence；此筆補上類型、原因與目標使用者，供管理端稽核而不曝露到一般用戶。
        if (!posting.replayed()) {
            jdbcTemplate.update(
                    "INSERT INTO audit_logs (actor_type, actor_id, action_type, target_type, target_id, request_id, outcome, reason) "
                            + "VALUES ('ADMIN', ?, 'ADMIN_ASSET_ADJUSTMENT', 'USER_ACCOUNT', ?, ?, 'SUCCESS', ?)",
                    actor.userId(), targetAccountId, "asset-adjustment-" + digest(businessReferenceId).substring(0, 40),
                    "type=AIRDROP;asset=" + command.assetSymbol() + ";amount="
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

    /** 只有明確標記的空投 funding account 可以承擔 issuance debit，不把 EXCHANGE 類型泛化成 overdraft 權限。 */
    private void ensureDesignatedAirdropFundingAccount() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_id, account_type, account_category, account_purpose, status FROM accounts WHERE account_id = ?",
                EXCHANGE_AIRDROP_ACCOUNT_ID
        );
        if (rows.size() != 1 || !EXCHANGE_AIRDROP_USER_ID.equals(rows.getFirst().get("user_id"))
                || !"SPOT".equals(rows.getFirst().get("account_type"))
                || !"EXCHANGE".equals(rows.getFirst().get("account_category"))
                || !"AIRDROP_FUNDING".equals(rows.getFirst().get("account_purpose"))
                || !"ACTIVE".equals(rows.getFirst().get("status"))) {
            throw new IllegalStateException("designated airdrop funding account policy is unavailable");
        }
    }

    private void ensureActiveAccountAsset(String accountId, String assetSymbol) {
        List<Map<String, Object>> assets = jdbcTemplate.queryForList(
                "SELECT status FROM assets WHERE asset_symbol = ?", assetSymbol
        );
        if (assets.size() != 1 || !"ACTIVE".equals(assets.getFirst().get("status"))) {
            throw new IllegalArgumentException("asset is not active for adjustment");
        }
        // account_assets 是 ledger entry 的前置關聯；只在 active asset 下建立，避免以資產調整繞過資產啟用流程。
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
            throw new IllegalArgumentException("account asset is not active for adjustment");
        }
    }

    private static String adjustmentReference(AdminAirdropCommand command) {
        return "asset-adjustment:" + command.activityId() + ":" + command.targetUserId() + ":"
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
