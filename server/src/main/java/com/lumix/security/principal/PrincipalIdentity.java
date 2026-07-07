package com.lumix.security.principal;

import java.util.Objects;

/**
 * principal 識別模型。
 *
 * 這裡只保留足夠辨識主體的安全欄位，不應混入 secret 或 signature payload。
 */
public record PrincipalIdentity(
    PrincipalType type,
    String principalId
) {

    public PrincipalIdentity {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(principalId, "principalId must not be null");
        principalId = principalId.trim();
        if (principalId.isEmpty()) {
            throw new IllegalArgumentException("principalId must not be blank");
        }
    }
}
