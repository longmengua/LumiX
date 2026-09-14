package com.lumix.admin.user;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * P27 唯讀使用者搜尋服務。
 *
 * <p>所有呼叫先由已啟用最高管理員的 server-side principal 驗證；UI session 與 email 設定均不能作為
 * 授權依據。本服務沒有任何使用者、角色、MFA、資產或帳本寫入命令。</p>
 */
@Service
class AdminUserQueryService {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_DISPLAY_NAME_PREFIX_LENGTH = 128;
    private static final int MAX_USER_ID_LENGTH = 64;

    private final AdminUserQueryRepository repository;
    private final SuperAdminAccessService superAdminAccess;

    AdminUserQueryService(AdminUserQueryRepository repository, SuperAdminAccessService superAdminAccess) {
        this.repository = repository;
        this.superAdminAccess = superAdminAccess;
    }

    /**
     * 以名稱前綴與註冊時間讀取使用者摘要。
     *
     * <p>這裡只接受顯示名稱，避免把多欄位 OR 模糊搜尋變成無法預期的全表掃描；兩個時間的 Before 都採排他上界，
     * 讓前端可安全表示完整的一天或精確時間區間。最後登入一律代表最新成功 session，而非任一歷史登入。</p>
     */
    AdminUserSearchPage find(
        AuthenticatedUser actor,
        String displayNamePrefix,
        Instant createdFrom,
        Instant createdBefore,
        Instant lastLoginFrom,
        Instant lastLoginBefore,
        Instant cursorCreatedAt,
        String cursorUserId,
        Integer limit
    ) {
        superAdminAccess.requireActiveSuperAdmin(actor);
        int pageSize = resolvePageSize(limit);
        AdminUserSearchCriteria criteria = new AdminUserSearchCriteria(
            normalizeDisplayNamePrefix(displayNamePrefix), createdFrom, createdBefore,
            lastLoginFrom, lastLoginBefore,
            resolveCursor(cursorCreatedAt, cursorUserId)
        );
        validateTimeRange(criteria.createdFrom(), criteria.createdBefore());
        validateTimeRange(criteria.lastLoginFrom(), criteria.lastLoginBefore());

        // 多讀一筆決定下一個 keyset cursor；total 則以相同篩選條件（但不含 cursor）提供真實頁碼資訊。
        List<AdminUserSummary> fetched = repository.find(criteria, pageSize + 1);
        long total = repository.count(criteria);
        boolean hasNext = fetched.size() > pageSize;
        List<AdminUserSummary> items = hasNext
            ? new ArrayList<>(fetched.subList(0, pageSize))
            : fetched;
        AdminUserSearchCursor nextCursor = hasNext ? cursorFor(items.getLast()) : null;
        return new AdminUserSearchPage(List.copyOf(items), nextCursor, total, pageSize);
    }

    AdminUserDetail detail(AuthenticatedUser actor, String userId) {
        superAdminAccess.requireActiveSuperAdmin(actor);
        return repository.findById(userId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }

    private static int resolvePageSize(Integer limit) {
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (resolvedLimit < 1 || resolvedLimit > MAX_LIMIT) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        return resolvedLimit;
    }

    private static String normalizeDisplayNamePrefix(String rawPrefix) {
        String normalized = rawPrefix == null ? "" : rawPrefix.strip();
        if (normalized.length() > MAX_DISPLAY_NAME_PREFIX_LENGTH) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        return normalized;
    }

    private static AdminUserSearchCursor resolveCursor(Instant createdAt, String userId) {
        boolean hasCreatedAt = createdAt != null;
        boolean hasUserId = userId != null && !userId.isBlank();
        if (hasCreatedAt != hasUserId || hasUserId && userId.length() > MAX_USER_ID_LENGTH) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        return hasCreatedAt ? new AdminUserSearchCursor(createdAt, userId) : null;
    }

    private static void validateTimeRange(Instant createdFrom, Instant createdBefore) {
        if (createdFrom != null && createdBefore != null && !createdFrom.isBefore(createdBefore)) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
    }

    private static AdminUserSearchCursor cursorFor(AdminUserSummary user) {
        return new AdminUserSearchCursor(user.createdAt(), user.userId());
    }
}
