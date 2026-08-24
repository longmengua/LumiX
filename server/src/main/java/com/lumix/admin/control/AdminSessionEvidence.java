package com.lumix.admin.control;
import com.lumix.account.UserId; import java.time.Instant; import java.util.Objects; import java.util.Set;
/** caller 提供的 immutable session/MFA/role evidence；不實作登入或 role assignment。 */
public record AdminSessionEvidence(UserId actor, Set<AdminRole> roles, boolean mfaVerified, Instant expiresAt) { public AdminSessionEvidence { actor=Objects.requireNonNull(actor,"actor"); roles=Set.copyOf(Objects.requireNonNull(roles,"roles")); expiresAt=Objects.requireNonNull(expiresAt,"expiresAt"); if(roles.isEmpty()) throw new IllegalArgumentException("admin role is required"); } }
