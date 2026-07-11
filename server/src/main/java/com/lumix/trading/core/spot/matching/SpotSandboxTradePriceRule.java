package com.lumix.trading.core.spot.matching;

/**
 * Spot sandbox 的 trade price rule。
 *
 * 這個 enum 只用來表達設計階段的價格規則與 review 狀態，不代表正式撮合已完成。
 */
public enum SpotSandboxTradePriceRule {
    MAKER_PRICE,
    HUMAN_REVIEW_REQUIRED
}
