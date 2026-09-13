package com.lumix.admin.superadmin;

/**
 * 密碼重設成功後啟用待啟用最高管理員的受限輸出邊界。
 *
 * <p>一般帳戶重設密碼不會有副作用；只有已存在的 PENDING_ACTIVATION principal 會被原子轉為 ACTIVE，
 * 因此此介面不能擴充成一般角色指派功能。</p>
 */
public interface SuperAdminActivationPort {

    void activateIfPending(String userId);

    /**
     * 後台密碼復原只接受已啟用的最高管理員，不能因為知道一般使用者 email 就寄發後台連結。
     * 預設拒絕讓沒有實作後台 principal 查驗的測試或替代 adapter 維持 fail-closed。
     */
    default boolean isActiveSuperAdmin(String userId) {
        return false;
    }
}
