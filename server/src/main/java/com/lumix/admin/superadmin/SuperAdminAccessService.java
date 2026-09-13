package com.lumix.admin.superadmin;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.user.auth.domain.AuthenticatedUser;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** 管理 API 的 server-side 授權閘門；前端畫面或 localStorage 一律不具授權效力。 */
@Service
@Profile("infrastructure")
public class SuperAdminAccessService {

    private final SuperAdminRepository repository;

    SuperAdminAccessService(SuperAdminRepository repository) {
        this.repository = repository;
    }

    /** 只有已啟用、且一般帳戶仍為 ACTIVE 的唯一最高管理員可讀取後台資料。 */
    public void requireActiveSuperAdmin(AuthenticatedUser actor) {
        if (!repository.isActiveSuperAdmin(actor.userId())) {
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        }
    }
}
