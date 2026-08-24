package com.lumix.launch;
/** 對應權威 readiness gate 的高階證據分類；不可由完成 foundation 自動視為已通過。 */
public enum LaunchReadinessGate { DATA_INTEGRITY, FUNDS_SAFETY, TRADING_SAFETY, SECURITY, OPERATIONS, BUSINESS_LAUNCH }
