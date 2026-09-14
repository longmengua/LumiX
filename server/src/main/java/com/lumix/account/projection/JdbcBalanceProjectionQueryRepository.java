package com.lumix.account.projection;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
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
 * PostgreSQL balance projection 唯讀 adapter。
 *
 * <p>query 從 balance_projections 出發，絕不以 account_assets 合成不存在的零餘額；因此 response 僅包含
 * 真正在資料庫 materialize 的 snapshot。owner predicate 綁在 accounts.user_id，不能藉 accountId 繞過。</p>
 */
@Repository
@Profile("infrastructure")
class JdbcBalanceProjectionQueryRepository implements BalanceProjectionQueryRepository {

    private static final String FIND_BY_OWNER_SQL = """
        SELECT a.account_id,
               a.account_type,
               a.status AS account_status,
               bp.asset_symbol,
               asset.display_name AS asset_display_name,
               asset.status AS asset_status,
               asset.precision_scale,
               bp.total_amount,
               bp.available_amount,
               bp.locked_amount,
               bp.projection_version,
               bp.projected_at,
               bp.reconciled_at
        FROM balance_projections bp
        JOIN accounts a ON a.account_id = bp.account_id
        JOIN assets asset ON asset.asset_symbol = bp.asset_symbol
        WHERE a.user_id = ?
        ORDER BY a.account_type ASC, bp.asset_symbol ASC, a.account_id ASC
        """;

    private final JdbcTemplate jdbcTemplate;

    JdbcBalanceProjectionQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AccountBalanceProjection> findByOwnerUserId(String userId) {
        return jdbcTemplate.query(
            FIND_BY_OWNER_SQL,
            (resultSet, rowNumber) -> new AccountBalanceProjection(
                new AccountId(resultSet.getString("account_id")),
                AccountType.valueOf(resultSet.getString("account_type")),
                AccountStatus.valueOf(resultSet.getString("account_status")),
                new AssetSymbol(resultSet.getString("asset_symbol")),
                resultSet.getString("asset_display_name"),
                resultSet.getString("asset_status"),
                resultSet.getInt("precision_scale"),
                new MoneyAmount(resultSet.getBigDecimal("total_amount")),
                new MoneyAmount(resultSet.getBigDecimal("available_amount")),
                new MoneyAmount(resultSet.getBigDecimal("locked_amount")),
                resultSet.getLong("projection_version"),
                toInstant(resultSet.getTimestamp("projected_at")),
                toInstant(resultSet.getTimestamp("reconciled_at"))
            ),
            userId
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
