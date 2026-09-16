package com.lumix.ledger.runtime;

import com.lumix.ledger.domain.LedgerEntryDraft;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 由 immutable ledger 推導餘額 read model 的同交易更新器。
 *
 * <p>此類別絕不接受「新餘額」作為輸入；每次只根據已 append 的 ledger entries 重新加總受影響的
 * account/asset，避免 projection 變成另一份可被命令直接竄改的資金真相。</p>
 */
@Component
public class LedgerBalanceProjectionUpdater {

    private final JdbcTemplate jdbcTemplate;

    public LedgerBalanceProjectionUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    /**
     * 重算本 journal 影響的 projection rows。
     *
     * <p>呼叫端必須在同一個 transaction 內先 append ledger entries。本輪尚未有 reservation runtime，
     * 所以 available 等於 total、locked 固定零；未來 reservation 必須取代這個 available 推導規則，
     * 不能直接覆蓋 total。</p>
     */
    public void refreshAffectedBalances(List<LedgerEntryDraft> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        Set<AccountAssetKey> affected = new LinkedHashSet<>();
        for (LedgerEntryDraft entry : entries) {
            affected.add(new AccountAssetKey(entry.accountId().value(), entry.assetSymbol().value()));
        }
        for (AccountAssetKey key : affected) {
            if (!isUserAccount(key.accountId())) {
                // 交易所對應帳戶維持在 ledger source of truth；絕不投影成某個使用者可見的資產。
                continue;
            }
            BigDecimal total = deriveTotalFromLedger(key);
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                // projection schema 不接受負數；這表示上游 debit 沒有 balance / reservation 證據，必須整筆回滾。
                throw new IllegalStateException("ledger-derived balance cannot be negative");
            }
            upsertProjection(key, total);
        }
    }

    private boolean isUserAccount(String accountId) {
        String category = jdbcTemplate.queryForObject(
                "SELECT account_category FROM accounts WHERE account_id = ?",
                String.class, accountId
        );
        if (category == null) {
            throw new IllegalStateException("ledger account must exist before projection refresh");
        }
        return "USER".equals(category);
    }

    private BigDecimal deriveTotalFromLedger(AccountAssetKey key) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END), 0) "
                        + "FROM ledger_entries WHERE account_id = ? AND asset_symbol = ?",
                BigDecimal.class, key.accountId(), key.assetSymbol()
        );
        return total == null ? BigDecimal.ZERO : total;
    }

    private void upsertProjection(AccountAssetKey key, BigDecimal total) {
        // reservation 是獨立 runtime；ledger replay 只能重算 total，不能把仍有效的 hold 靜默清零。
        jdbcTemplate.update(
                "INSERT INTO balance_projections "
                        + "(account_id, asset_symbol, total_amount, available_amount, locked_amount, projection_version, projected_at, reconciled_at) "
                        + "VALUES (?, ?, ?, ?, 0, 1, CURRENT_TIMESTAMP, NULL) "
                        + "ON CONFLICT (account_id, asset_symbol) DO UPDATE SET "
                        + "total_amount = EXCLUDED.total_amount, "
                        + "available_amount = EXCLUDED.total_amount - balance_projections.locked_amount, "
                        + "locked_amount = balance_projections.locked_amount, "
                        + "projection_version = balance_projections.projection_version + 1, "
                        + "projected_at = CURRENT_TIMESTAMP, reconciled_at = NULL",
                key.accountId(), key.assetSymbol(), total, total
        );
    }

    private record AccountAssetKey(String accountId, String assetSymbol) {
    }
}
