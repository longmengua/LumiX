package com.lumix.risk.control;
import java.util.Objects;
/** 將保護 signal 明確轉成純 decision，避免未來 runtime 把 read evidence 誤當成可 bypass 的提示。 */
public final class RiskProtectionPolicy { public RiskDecision evaluate(RiskProtectionSignal signal) { return Objects.requireNonNull(signal,"signal")==RiskProtectionSignal.NONE ? RiskDecision.ALLOWED : RiskDecision.MARKET_PROTECTION_REJECTED; } }
