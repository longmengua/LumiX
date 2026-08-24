package com.lumix.withdrawal.signing;

import com.lumix.withdrawal.request.WithdrawalNetwork;
import java.util.Objects;
import java.util.Set;

/** 可用 signer 的唯讀 capability evidence；network allowlist 防止 intent 送往不相容 signer。 */
public record WithdrawalSignerCapability(String capabilityId, WithdrawalSignerProviderKind providerKind, Set<String> supportedNetworkCodes, boolean available) {
    public WithdrawalSignerCapability {
        capabilityId = Objects.requireNonNull(capabilityId, "capabilityId").trim();
        providerKind = Objects.requireNonNull(providerKind, "providerKind");
        supportedNetworkCodes = Set.copyOf(Objects.requireNonNull(supportedNetworkCodes, "supportedNetworkCodes"));
        if (capabilityId.isEmpty() || supportedNetworkCodes.isEmpty()) throw new IllegalArgumentException("capability identity and network allowlist are required");
    }
    /** capability 只允許依 network code 比對，避免接觸 destination 或任何 key material。 */
    public boolean supports(WithdrawalNetwork network) { return supportedNetworkCodes.contains(Objects.requireNonNull(network, "network").code()); }
}
