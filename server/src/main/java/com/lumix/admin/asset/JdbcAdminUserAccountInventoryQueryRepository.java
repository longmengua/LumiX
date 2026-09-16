package com.lumix.admin.asset;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AccountType;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 管理端帳戶容器的 PostgreSQL 唯讀 adapter。
 *
 * <p>SQL 固定以目標使用者作為 owner 篩選，且不 join projection 補造零餘額；這讓支援人員能分辨
 * 帳戶尚無資產資料與帳戶根本不存在兩種不同營運狀態。</p>
 */
@Repository
@Profile("infrastructure")
class JdbcAdminUserAccountInventoryQueryRepository implements AdminUserAccountInventoryQueryRepository {
    private static final String SQL = """
        SELECT account_id, account_type, status, created_at
        FROM accounts
        WHERE user_id = ?
        ORDER BY account_type ASC, account_id ASC
        """;

    private final JdbcTemplate jdbcTemplate;

    JdbcAdminUserAccountInventoryQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AdminUserAccountInventory> findByUserId(String userId) {
        return jdbcTemplate.query(SQL, (resultSet, rowNumber) -> new AdminUserAccountInventory(
            new AccountId(resultSet.getString("account_id")),
            AccountType.valueOf(resultSet.getString("account_type")),
            AccountStatus.valueOf(resultSet.getString("status")),
            resultSet.getTimestamp("created_at").toInstant()
        ), userId);
    }
}
