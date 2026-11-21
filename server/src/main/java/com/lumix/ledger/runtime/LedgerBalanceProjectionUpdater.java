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
     * <p>呼叫端必須在同一個 transaction 內先 append ledger entries。先鎖所有受影響的投影，再由
     * ledger 重算 total 並保留有效 locked，避免兩個交易在等鎖前各自計算舊總額而發生最後寫入覆蓋。</p>
     */
    public void refreshAffectedBalances(List<LedgerEntryDraft> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        Set<AccountAssetKey> affected = new LinkedHashSet<>();
        for (LedgerEntryDraft entry : entries) {
            affected.add(new AccountAssetKey(entry.accountId().value(), entry.assetSymbol().value()));
        }
        List<AccountAssetKey> ordered = affected.stream().sorted(java.util.Comparator.comparing(AccountAssetKey::accountId)
                .thenComparing(AccountAssetKey::assetSymbol)).filter(key -> isUserAccount(key.accountId())).toList();
        for (AccountAssetKey key : ordered) {
            // 新收款資產需先有 transaction 局部鎖 row；零值不會作為對外餘額，提交前必由 immutable ledger 重算。
            // 缺投影卻存在 reservation 是資料缺口，不能把保留額初始化為零而放寬可用額。
            Integer missingHold = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservations r WHERE r.account_id=? AND r.asset_symbol=? "
                    + "AND r.remaining_amount>0 AND NOT EXISTS (SELECT 1 FROM balance_projections p WHERE p.account_id=r.account_id AND p.asset_symbol=r.asset_symbol)",
                    Integer.class, key.accountId(), key.assetSymbol());
            if (missingHold != null && missingHold > 0) throw new IllegalStateException("reservation projection is missing");
            jdbcTemplate.update("INSERT INTO balance_projections(account_id,asset_symbol,total_amount,available_amount,locked_amount,projection_version,projected_at) "
                    + "VALUES (?,?,0,0,0,0,CURRENT_TIMESTAMP) ON CONFLICT(account_id,asset_symbol) DO NOTHING", key.accountId(),key.assetSymbol());
            jdbcTemplate.queryForObject("SELECT projection_version FROM balance_projections WHERE account_id=? AND asset_symbol=? FOR UPDATE",Long.class,key.accountId(),key.assetSymbol());
        }
        for (AccountAssetKey key : ordered) {
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
