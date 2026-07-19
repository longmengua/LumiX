package com.lumix.trading.core.futures.sandbox.insurance;

import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Phase 19-T03 的 immutable insurance-fund sandbox placeholder。
 *
 * 此型別只提供後續 simulation/reconciliation test 使用的明確 snapshot，
 * 不代表保險基金帳戶、資產歸集、賠付授權或任何真實資金能力。
 */
public record FuturesSandboxInsuranceFundPlaceholder(
        AssetSymbol asset,
        MoneyAmount simulatedAmount,
        Instant observedAt
) {
    public FuturesSandboxInsuranceFundPlaceholder {
        Objects.requireNonNull(asset, "asset must not be null");
        Objects.requireNonNull(simulatedAmount, "simulatedAmount must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        if (simulatedAmount.value().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("simulatedAmount must not be negative");
        }
    }
}
