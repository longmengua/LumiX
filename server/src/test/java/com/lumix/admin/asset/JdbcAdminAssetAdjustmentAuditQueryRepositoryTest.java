package com.lumix.admin.asset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** 驗證對賬 adapter 只讀取既有證據，不能把查詢路徑變成帳務修正入口。 */
class JdbcAdminAssetAdjustmentAuditQueryRepositoryTest {

    @Test
    void crossChecksAuditJournalsAndEntriesWithoutMutation() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        new JdbcAdminAssetAdjustmentAuditQueryRepository(jdbcTemplate).findLatest(100);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), org.mockito.ArgumentMatchers.<RowMapper<AdminAssetAdjustmentAuditItem>>any(), org.mockito.ArgumentMatchers.eq(100));
        assertTrue(sql.getValue().contains("FROM admin_asset_adjustments adjustment"));
        assertTrue(sql.getValue().contains("LEFT JOIN ledger_entries entry"));
        assertTrue(sql.getValue().contains("reconciliation_status"));
        assertFalse(sql.getValue().matches("(?s).*\\b(INSERT|UPDATE|DELETE)\\b.*"));
    }
}
