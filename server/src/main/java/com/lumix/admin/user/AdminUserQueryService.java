package com.lumix.admin.user;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.user.auth.domain.AuthenticatedUser;
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

    private final AdminUserQueryRepository repository;
    private final SuperAdminAccessService superAdminAccess;

    AdminUserQueryService(AdminUserQueryRepository repository, SuperAdminAccessService superAdminAccess) {
        this.repository = repository;
        this.superAdminAccess = superAdminAccess;
    }

    List<AdminUserSummary> find(AuthenticatedUser actor, String query, Integer limit) {
        superAdminAccess.requireActiveSuperAdmin(actor);
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (resolvedLimit < 1 || resolvedLimit > MAX_LIMIT) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
        return repository.find(query == null ? "" : query.trim(), resolvedLimit);
    }

    AdminUserDetail detail(AuthenticatedUser actor, String userId) {
        superAdminAccess.requireActiveSuperAdmin(actor);
        return repository.findById(userId).orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }
}
