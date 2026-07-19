package com.lumix.trading.core.futures.sandbox.contract;

import com.lumix.trading.core.futures.order.FuturesSandboxOrder;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import java.util.Objects;

/**
 * 已通過受限 contract eligibility gate 的 immutable inspection snapshot。
 *
 * snapshot 只保留前序 task 已產生的 accepted order 與人工 mark-price input，
 * 不能作為 fill、trade、position update、PnL/funding 套用或任何帳務異動的指令。
 */
public record FuturesSandboxContractInspection(
        RestrictedFuturesSandboxContract contract,
        FuturesSandboxOrder acceptedOrder,
        FuturesSandboxMockMarkPrice markPrice
) {

    public FuturesSandboxContractInspection {
        Objects.requireNonNull(contract, "contract must not be null");
        Objects.requireNonNull(acceptedOrder, "acceptedOrder must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
    }
}
