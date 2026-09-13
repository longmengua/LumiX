package com.lumix.admin.user;

import java.util.List;
import java.util.Optional;

/** 使用者管理只讀投影的持久化介面，禁止加入停權、角色或帳務 mutation。 */
interface AdminUserQueryRepository {

    List<AdminUserSummary> find(String query, int limit);

    Optional<AdminUserDetail> findById(String userId);
}
