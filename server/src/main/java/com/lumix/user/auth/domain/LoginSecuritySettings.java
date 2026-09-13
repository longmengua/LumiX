package com.lumix.user.auth.domain;

/** 使用者本人可維護的新裝置登入通知偏好；不是權限、MFA 或風控決策模型。 */
public record LoginSecuritySettings(boolean newDeviceLoginEmailNotificationEnabled) { }
