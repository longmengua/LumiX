package com.lumix.trading.core.futures.sandbox.market;

/**
 * Futures sandbox mark price 的來源標記。
 *
 * T05 只允許明確由測試或受限 sandbox 呼叫端提供的人工輸入，避免被誤接到尚未核准的正式行情來源。
 */
public enum FuturesSandboxMockMarkPriceSource {
    MANUAL_SANDBOX_INPUT
}
