package com.lumix.admin.user;

import java.util.List;

/** 使用者詳情只補足帳戶安全說明所需的裝置摘要，避免後台暴露可重放憑證。 */
public record AdminUserDetail(AdminUserSummary user, List<AdminUserDevice> devices) { }
