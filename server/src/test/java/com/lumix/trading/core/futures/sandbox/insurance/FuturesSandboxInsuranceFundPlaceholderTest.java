package com.lumix.trading.core.futures.sandbox.insurance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 驗證 insurance fund placeholder 只接受非負的模擬 snapshot。 */
class FuturesSandboxInsuranceFundPlaceholderTest {
    @Test
    void keepsExplicitSimulationSnapshotAndRejectsNegativeAmount() {
        var placeholder = new FuturesSandboxInsuranceFundPlaceholder(new AssetSymbol("USDT"), new MoneyAmount(new BigDecimal("100")), Instant.parse("2026-07-20T00:00:00Z"));
        assertEquals(new AssetSymbol("USDT"), placeholder.asset());
        assertEquals(0, new BigDecimal("100").compareTo(placeholder.simulatedAmount().value()));
        assertEquals("simulatedAmount must not be negative", assertThrows(IllegalArgumentException.class, () -> new FuturesSandboxInsuranceFundPlaceholder(new AssetSymbol("USDT"), new MoneyAmount(new BigDecimal("-1")), Instant.now())).getMessage());
    }
}
