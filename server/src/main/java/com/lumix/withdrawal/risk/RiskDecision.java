package com.lumix.withdrawal.risk;

/** 風控不可只回傳 boolean；HOLD 表示依賴或證據未知，必須 fail closed。 */
public enum RiskDecision { ALLOW, MANUAL_REVIEW, DENY, HOLD }
