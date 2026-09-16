package com.lumix.account.projection;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AccountType;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** PostgreSQL 帳戶容器唯讀 adapter；owner predicate 在 SQL 層固定，不能接受 browser accountId。 */
@Repository @Profile("infrastructure")
class JdbcAccountInventoryQueryRepository implements AccountInventoryQueryRepository {
    private final JdbcTemplate jdbcTemplate;
    JdbcAccountInventoryQueryRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override public List<AccountInventoryItem> findByOwnerUserId(String userId) {
        return jdbcTemplate.query("SELECT account_id, account_type, status, created_at FROM accounts WHERE user_id = ? ORDER BY account_type ASC", (rs, row) ->
            new AccountInventoryItem(new AccountId(rs.getString(1)), AccountType.valueOf(rs.getString(2)), AccountStatus.valueOf(rs.getString(3)), rs.getTimestamp(4).toInstant()), userId);
    }
}
