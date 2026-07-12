package com.lumix.trading.core.spot.settlement;

import com.lumix.trading.core.spot.matching.SpotSandboxSettlementInput;
import java.util.Objects;

/**
 * Spot sandbox settlement runtime boundary。
 *
 * 這個 boundary 只允許 sandbox-only settlement runtime gate，不代表正式 settlement engine、ledger posting 或 balance refresh 已完成。
 */
public final class SpotSandboxSettlementRuntimeBoundary {

    private final SpotSandboxSettlementRuntimeGate gate;

    /**
     * 建立 sandbox settlement runtime boundary。
     *
     * 這裡只接 runtime gate，不接任何 DB client、ledger posting service 或 balance refresh runtime。
     */
    public SpotSandboxSettlementRuntimeBoundary() {
        this.gate = new SpotSandboxSettlementRuntimeGate();
    }

    /**
     * 針對單一 settlement input 建立 sandbox settlement plan。
     *
     * 這個方法只回傳 sandbox plan，不代表 settlement completed、ledger posted 或 balance updated。
     */
    public SpotSandboxSettlementRuntimeResult plan(SpotSandboxSettlementInput settlementInput) {
        return gate.plan(Objects.requireNonNull(settlementInput, "settlementInput must not be null"));
    }
}
