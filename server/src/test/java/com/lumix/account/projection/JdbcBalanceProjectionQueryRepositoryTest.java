package com.lumix.account.projection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcBalanceProjectionQueryRepositoryTest {

    /** SQL 必須在資料庫層綁 owner predicate，且只讀現有 projection，不能讀取 account_assets 來合成假餘額。 */
    @Test
    void scopesTheReadOnlyProjectionQueryToTheOwner() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcBalanceProjectionQueryRepository repository = new JdbcBalanceProjectionQueryRepository(jdbcTemplate);

        repository.findByOwnerUserId("user-owner");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), org.mockito.ArgumentMatchers.<RowMapper<AccountBalanceProjection>>any(),
            org.mockito.ArgumentMatchers.eq("user-owner"));
        assertTrue(sql.getValue().contains("FROM balance_projections bp"));
        assertTrue(sql.getValue().contains("WHERE a.user_id = ?"));
        assertTrue(sql.getValue().contains("JOIN accounts a ON a.account_id = bp.account_id"));
        assertFalse(sql.getValue().contains("account_assets"));
        assertFalse(sql.getValue().contains("INSERT"));
        assertFalse(sql.getValue().contains("UPDATE"));
        assertFalse(sql.getValue().contains("DELETE"));
    }
}
