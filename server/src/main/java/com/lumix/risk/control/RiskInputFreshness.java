package com.lumix.risk.control;
/** 風控輸入的可信度；非 FRESH 一律不可被 policy 當成允許依據。 */
public enum RiskInputFreshness { FRESH, STALE, GAP, UNKNOWN }
