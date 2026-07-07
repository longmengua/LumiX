package com.lumix.security.principal;

/**
 * principal 類型。
 *
 * 這個模型只描述主體身份類別，不代表登入或 token 驗證已完成。
 */
public enum PrincipalType {
    USER,
    ADMIN,
    SERVICE
}
