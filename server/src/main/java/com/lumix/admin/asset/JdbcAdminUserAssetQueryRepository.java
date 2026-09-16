package com.lumix.admin.asset;

import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 管理端使用者資產 projection 的 PostgreSQL 唯讀 adapter。
 *
 * <p>此查詢只讀現有 materialized projection，不能由 account_assets 補零或直接讀 ledger 推導餘額；管理端仍須
 * 將結果視為 read model evidence，而非手動調帳的依據。</p>
 */
@Repository
@Profile("infrastructure")
class JdbcAdminUserAssetQueryRepository implements AdminUserAssetQueryRepository {
    private static final String SQL = """
        SELECT account.account_type, projection.asset_symbol, projection.total_amount, projection.available_amount,
               projection.locked_amount, projection.projection_version, projection.projected_at, projection.reconciled_at
        FROM balance_projections projection
        JOIN accounts account ON account.account_id = projection.account_id
        WHERE account.user_id = ?
        ORDER BY account.account_type ASC, projection.asset_symbol ASC, account.account_id ASC
        """;
    private final JdbcTemplate jdbcTemplate;
    JdbcAdminUserAssetQueryRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override public List<AdminUserAssetProjection> findByUserId(String userId) {
        return jdbcTemplate.query(SQL, (resultSet, rowNumber) -> new AdminUserAssetProjection(
            AccountType.valueOf(resultSet.getString("account_type")), new AssetSymbol(resultSet.getString("asset_symbol")),
            new MoneyAmount(resultSet.getBigDecimal("total_amount")), new MoneyAmount(resultSet.getBigDecimal("available_amount")),
            new MoneyAmount(resultSet.getBigDecimal("locked_amount")), resultSet.getLong("projection_version"),
            toInstant(resultSet.getTimestamp("projected_at")), toInstant(resultSet.getTimestamp("reconciled_at"))
        ), userId);
    }
    private static Instant toInstant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
