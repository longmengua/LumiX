package com.lumix.admin.asset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcAdminUserAssetQueryRepositoryTest {
    /** 管理端資產頁只能讀現有 projection，不能因為支援需求加入任何 balance 或 ledger mutation。 */
    @Test void readsOwnerProjectionWithoutMutation() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        new JdbcAdminUserAssetQueryRepository(jdbcTemplate).findByUserId("target-user");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), org.mockito.ArgumentMatchers.<RowMapper<AdminUserAssetProjection>>any(), org.mockito.ArgumentMatchers.eq("target-user"));
        assertTrue(sql.getValue().contains("FROM balance_projections projection"));
        assertTrue(sql.getValue().contains("WHERE account.user_id = ?"));
        assertFalse(sql.getValue().matches("(?s).*\\b(INSERT|UPDATE|DELETE)\\b.*"));
    }
}
