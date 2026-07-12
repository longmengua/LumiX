package com.lumix.trading.core.spot.matching;

/**
 * Spot sandbox matching runtime 的決策結果。
 *
 * 這個 decision 只表示 sandbox matcher 是否產生 trade / fill，不能被誤解成正式撮合已上線。
 */
public enum SpotSandboxMatchDecision {
    MATCHED,
    NO_MATCH,
    REJECTED
}
