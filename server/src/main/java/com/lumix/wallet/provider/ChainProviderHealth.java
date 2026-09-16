package com.lumix.wallet.provider;

import java.time.Instant;
import java.util.Objects;

/** provider health evidence；不以「沒有錯誤」推測為可安全入帳。 */
public record ChainProviderHealth(boolean available, Instant observedAt, String evidenceReference) {
    public ChainProviderHealth {
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(evidenceReference, "evidenceReference must not be null");
        evidenceReference = evidenceReference.trim();
        if (evidenceReference.isEmpty()) throw new IllegalArgumentException("evidenceReference must not be blank");
    }
}
