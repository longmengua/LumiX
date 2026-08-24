package com.lumix.risk.control;
import java.math.BigInteger; import java.time.Instant; import java.util.Objects;
/** 版本化且有生效時間的限額規則；不包含任何 mutable consumption counter。 */
public record RiskPolicy(String version, BigInteger maximumAtomicAmount, Instant effectiveAt) { public RiskPolicy { version=Objects.requireNonNull(version,"version").trim(); maximumAtomicAmount=Objects.requireNonNull(maximumAtomicAmount,"maximumAtomicAmount"); effectiveAt=Objects.requireNonNull(effectiveAt,"effectiveAt"); if(version.isEmpty()||maximumAtomicAmount.signum()<=0) throw new IllegalArgumentException("policy version and positive limit required"); } }
