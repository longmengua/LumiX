package com.lumix.admin.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcAdminUserQueryRepositoryTest {

    @Test
    void countsTheSameFiltersWithoutApplyingTheKeysetCursor() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcAdminUserQueryRepository repository = new JdbcAdminUserQueryRepository(jdbcTemplate);
        AdminUserSearchCriteria criteria = new AdminUserSearchCriteria(
            "Lin", Instant.parse("2026-09-01T00:00:00Z"), null,
            Instant.parse("2026-09-10T00:00:00Z"), null,
            new AdminUserSearchCursor(Instant.parse("2026-09-15T00:00:00Z"), "user-15")
        );
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(Long.class),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(8L);

        assertEquals(8L, repository.count(criteria));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
            sql.capture(), org.mockito.ArgumentMatchers.eq(Long.class), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
        // total 必須保留篩選，卻不能把「本頁以後」的 keyset 條件誤當成完整總數。
        assertTrue(sql.getValue().contains("lower(u.display_name) LIKE ? ESCAPE '\\'"));
        assertTrue(sql.getValue().contains("login_history.last_login_at >= ?"));
        assertFalse(sql.getValue().contains("(u.created_at, u.user_id) < (?, ?)"));
    }

    @Test
    void usesEscapedSuffixOnlyNamePrefixAndKeysetPredicate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcAdminUserQueryRepository repository = new JdbcAdminUserQueryRepository(jdbcTemplate);
        AdminUserSearchCriteria criteria = new AdminUserSearchCriteria(
            "Lin%_", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-09-10T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"),
            new AdminUserSearchCursor(Instant.parse("2026-09-15T00:00:00Z"), "user-15")
        );

        repository.find(criteria, 26);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(
            sql.capture(), org.mockito.ArgumentMatchers.<RowMapper<AdminUserSummary>>any(), arguments.capture()
        );

        // 查詢不允許 %keyword%：前綴萬用字元會令 PostgreSQL 無法使用名稱 prefix index。
        assertTrue(sql.getValue().contains("lower(u.display_name) LIKE ? ESCAPE '\\'"));
        assertTrue(sql.getValue().contains("login_history.last_login_at >= ?"));
        assertTrue(sql.getValue().contains("login_history.last_login_at < ?"));
        // 限制色點必須由資料庫已知的帳戶與資金限制狀態聚合，不能讓前端從其他欄位推測。
        assertTrue(sql.getValue().contains("a.status = 'FROZEN'"));
        assertTrue(sql.getValue().contains("AS has_active_restriction"));
        assertTrue(sql.getValue().contains("(u.created_at, u.user_id) < (?, ?)"));
        assertTrue(sql.getValue().contains("ORDER BY u.created_at DESC, u.user_id DESC LIMIT ?"));
        assertEquals("lin\\%\\_%", arguments.getValue()[0]);
        assertFalse(((String) arguments.getValue()[0]).startsWith("%"));
        assertEquals(26, arguments.getValue()[arguments.getValue().length - 1]);
    }
}
