package com.lumix.admin.asset;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** 所有管理端資產資料先經 server-side super-admin 授權，再執行 bounded readonly query。 */
@Service
class AdminUserAssetQueryService {
    private final AdminUserAssetQueryRepository repository;
    private final AdminUserLedgerHistoryQueryRepository historyRepository;
    private final AdminUserAccountInventoryQueryRepository accountInventoryRepository;
    private final SuperAdminAccessService access;
    AdminUserAssetQueryService(AdminUserAssetQueryRepository repository, AdminUserLedgerHistoryQueryRepository historyRepository,
                               AdminUserAccountInventoryQueryRepository accountInventoryRepository,
                               SuperAdminAccessService access) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.accountInventoryRepository = accountInventoryRepository;
        this.access = access;
    }
    List<AdminUserAssetProjection> find(AuthenticatedUser actor, String userId) {
        access.requireActiveSuperAdmin(actor);
        String target = Objects.requireNonNull(userId, "userId must not be null").trim();
        if (target.isEmpty() || target.length() > 64) throw new IllegalArgumentException("userId must be canonical");
        return List.copyOf(repository.findByUserId(target));
    }
    List<AdminUserLedgerHistoryItem> history(AuthenticatedUser actor, String userId) { access.requireActiveSuperAdmin(actor); return List.copyOf(historyRepository.findLatestByUserId(canonicalUserId(userId), 20)); }
    /** 先完成 server-side RBAC，再讀取帳戶容器，不能讓 client 以公開 userId 探測帳戶存在性。 */
    List<AdminUserAccountInventory> accounts(AuthenticatedUser actor, String userId) {
        access.requireActiveSuperAdmin(actor);
        return List.copyOf(accountInventoryRepository.findByUserId(canonicalUserId(userId)));
    }
    private static String canonicalUserId(String userId) {
        String target = Objects.requireNonNull(userId, "userId must not be null").trim();
        if (target.isEmpty() || target.length() > 64) throw new IllegalArgumentException("userId must be canonical");
        return target;
    }
}
