package com.lumix.risk.control;
/** deterministic policy outcome；ALLOWED 不授權 runtime command 或資金異動。 */
public enum RiskDecision { ALLOWED, INPUT_NOT_TRUSTED_REJECTED, MARKET_PROTECTION_REJECTED, LIMIT_EXCEEDED_REJECTED }
