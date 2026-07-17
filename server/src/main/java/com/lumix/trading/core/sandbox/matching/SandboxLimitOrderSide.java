package com.lumix.trading.core.sandbox.matching;

/**
 * 共用 sandbox 限價單的買賣方向。
 *
 * 此型別只讓 spot 與 futures 共用純配對規則，不承載市場、倉位或結算語意。
 */
public enum SandboxLimitOrderSide {
    BUY,
    SELL
}
