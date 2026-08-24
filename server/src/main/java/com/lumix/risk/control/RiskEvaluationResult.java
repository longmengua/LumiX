package com.lumix.risk.control;
import java.util.Objects;
/** 結果含 policy version 供 audit/replay，不保存或消耗任何額度。 */
public record RiskEvaluationResult(RiskDecision decision, String policyVersion) { public RiskEvaluationResult { decision=Objects.requireNonNull(decision,"decision"); policyVersion=Objects.requireNonNull(policyVersion,"policyVersion"); } }
