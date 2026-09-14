package com.lumix.admin.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.api.error.ApiException;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminUserQueryServiceTest {

    @Test
    void returnsABoundedPageAndStableCursorForNamePrefixAndTimeRange() {
        AdminUserQueryRepository repository = mock(AdminUserQueryRepository.class);
        SuperAdminAccessService accessService = mock(SuperAdminAccessService.class);
        AdminUserQueryService service = new AdminUserQueryService(repository, accessService);
        AuthenticatedUser actor = new AuthenticatedUser("admin-1", "admin@example.com", "Admin");
        List<AdminUserSummary> fetched = List.of(
            user("user-3", "2026-09-03T00:00:00Z"),
            user("user-2", "2026-09-02T00:00:00Z"),
            user("user-1", "2026-09-01T00:00:00Z")
        );
        when(repository.find(any(), eq(3))).thenReturn(fetched);
        when(repository.count(any())).thenReturn(8L);

        AdminUserSearchPage page = service.find(
            actor, "  Lin%_  ", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"),
            Instant.parse("2026-09-10T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"), null, null, 2
        );

        ArgumentCaptor<AdminUserSearchCriteria> criteria = ArgumentCaptor.forClass(AdminUserSearchCriteria.class);
        verify(repository).find(criteria.capture(), eq(3));
        verify(accessService).requireActiveSuperAdmin(actor);
        // 名稱中的 SQL 萬用字元保留為純文字；repository 才能統一在尾端加入唯一的 %。
        assertEquals("Lin%_", criteria.getValue().displayNamePrefix());
        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), criteria.getValue().createdFrom());
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), criteria.getValue().createdBefore());
        assertEquals(Instant.parse("2026-09-10T00:00:00Z"), criteria.getValue().lastLoginFrom());
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), criteria.getValue().lastLoginBefore());
        assertEquals(List.of(fetched.get(0), fetched.get(1)), page.items());
        assertEquals(new AdminUserSearchCursor(Instant.parse("2026-09-02T00:00:00Z"), "user-2"), page.nextCursor());
        assertEquals(8, page.total());
        assertEquals(2, page.pageSize());
        verify(repository).count(criteria.getValue());
    }

    @Test
    void acceptsLastPageWithoutCreatingAnEmptyCursor() {
        AdminUserQueryRepository repository = mock(AdminUserQueryRepository.class);
        when(repository.find(any(), eq(3))).thenReturn(List.of(user("user-1", "2026-09-01T00:00:00Z")));
        when(repository.count(any())).thenReturn(1L);
        AdminUserQueryService service = new AdminUserQueryService(repository, mock(SuperAdminAccessService.class));

        AdminUserSearchPage page = service.find(
            new AuthenticatedUser("admin-1", "admin@example.com", "Admin"), null, null, null, null, null, null, null, 2
        );

        // 最後一頁沒有下一頁時不可回傳游標，否則前端會反覆送出相同查詢。
        assertEquals(1, page.items().size());
        assertNull(page.nextCursor());
        assertEquals(1, page.total());
    }

    @Test
    void rejectsIncompleteCursorAndInvalidTimeRangeBeforeReadingUsers() {
        AdminUserQueryRepository repository = mock(AdminUserQueryRepository.class);
        AdminUserQueryService service = new AdminUserQueryService(repository, mock(SuperAdminAccessService.class));
        AuthenticatedUser actor = new AuthenticatedUser("admin-1", "admin@example.com", "Admin");

        assertThrows(ApiException.class, () -> service.find(
            actor, "Lin", null, null, null, null, Instant.parse("2026-09-01T00:00:00Z"), null, 25
        ));
        assertThrows(ApiException.class, () -> service.find(
            actor, "Lin", Instant.parse("2026-09-02T00:00:00Z"), Instant.parse("2026-09-01T00:00:00Z"), null, null, null, null, 25
        ));
        assertThrows(ApiException.class, () -> service.find(
            actor, "Lin", null, null, Instant.parse("2026-09-02T00:00:00Z"), Instant.parse("2026-09-01T00:00:00Z"), null, null, 25
        ));
    }

    private static AdminUserSummary user(String userId, String createdAt) {
        return new AdminUserSummary(
            userId, userId + "@example.com", "Lin", "ACTIVE", Instant.parse(createdAt), null, null, false
        );
    }
}
