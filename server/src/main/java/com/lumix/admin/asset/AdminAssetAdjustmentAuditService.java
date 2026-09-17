package com.lumix.admin.asset;

import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 管理端資產調整對賬服務。
 *
 * <p>先執行 server-side 最高管理員授權，再以固定上限讀取歷史資料；這個服務沒有任何
 * balance、ledger 或 audit mutation 能力。</p>
 */
@Service
class AdminAssetAdjustmentAuditService {
    private static final int MAXIMUM_ITEMS = 100;
    private final AdminAssetAdjustmentAuditQueryRepository repository;
    private final SuperAdminAccessService access;

    AdminAssetAdjustmentAuditService(AdminAssetAdjustmentAuditQueryRepository repository, SuperAdminAccessService access) {
        this.repository = repository;
        this.access = access;
    }

    List<AdminAssetAdjustmentAuditItem> findLatest(AuthenticatedUser actor) {
        access.requireActiveSuperAdmin(actor);
        return List.copyOf(repository.findLatest(MAXIMUM_ITEMS));
    }
}
