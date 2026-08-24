package com.lumix.admin.control;
/** default-deny admin decision；ALLOWED 只表示可建立 request/review evidence，不執行 command。 */
public enum AdminAuthorizationDecision { ALLOWED, SESSION_EXPIRED_REJECTED, MFA_REQUIRED_REJECTED, ROLE_NOT_ALLOWED_REJECTED }
