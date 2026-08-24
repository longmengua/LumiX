package com.lumix.risk.control;
/** 唯讀市場保護狀態；HALTED/UNKNOWN 只影響決策結果，不會執行 circuit breaker。 */
public enum RiskMarketState { NORMAL, HALTED, UNKNOWN }
