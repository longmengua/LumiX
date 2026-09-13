package com.lumix.admin.superadmin;

import com.lumix.user.auth.domain.AuthenticatedUser;

/** 資料庫最高管理員的去敏投影；絕不攜帶 password hash、session 或 reset token。 */
record SuperAdminPrincipal(AuthenticatedUser user, State state) {

    enum State {
        PENDING_ACTIVATION,
        ACTIVE
    }
}
