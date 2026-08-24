package com.lumix.deposit.credit;

import com.lumix.deposit.address.DepositAddressLifecycle;
import com.lumix.deposit.observation.finality.DepositObservationLifecycle;
import java.util.Objects;

/**
 * P23-T01 的 deterministic、fail-closed credit eligibility policy。
 *
 * <p>判定順序固定且全程唯讀。任何不一致皆回傳具名拒絕原因，不修改 candidate、ownership、ledger 或 balance。</p>
 */
public final class DepositCreditEligibilityPolicy {

    public DepositCreditDecision evaluate(DepositCreditCandidate candidate, DepositCreditAssetNetworkPolicy policy) {
        candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        policy = Objects.requireNonNull(policy, "policy must not be null");
        if (!policy.enabled()) {
            return denied(DepositCreditDecisionReason.POLICY_DISABLED, policy);
        }
        if (!candidate.evidencePolicyVersion().equals(policy.version())) {
            return denied(DepositCreditDecisionReason.POLICY_VERSION_MISMATCH, policy);
        }
        if (!candidate.observationEvidence().observation().network().equals(policy.network())
                || !candidate.ownership().network().equals(policy.network())) {
            return denied(DepositCreditDecisionReason.NETWORK_MISMATCH, policy);
        }
        if (!candidate.observationEvidence().observation().asset().equals(policy.asset())
                || !candidate.ownership().asset().equals(policy.asset())) {
            return denied(DepositCreditDecisionReason.ASSET_MISMATCH, policy);
        }
        if (!candidate.observationEvidence().observation().recipientAddress().equals(candidate.ownership().address())) {
            return denied(DepositCreditDecisionReason.RECIPIENT_ADDRESS_MISMATCH, policy);
        }
        if (candidate.ownership().lifecycle() != DepositAddressLifecycle.ACTIVE) {
            return denied(DepositCreditDecisionReason.OWNERSHIP_NOT_ACTIVE, policy);
        }
        if (candidate.observationEvidence().finalityState().lifecycle()
                != DepositObservationLifecycle.FINALITY_THRESHOLD_MET) {
            return denied(DepositCreditDecisionReason.FINALITY_NOT_MET, policy);
        }
        if (candidate.observationEvidence().finalityState().confirmationCount() < policy.requiredConfirmations().value()) {
            return denied(DepositCreditDecisionReason.INSUFFICIENT_CONFIRMATIONS, policy);
        }
        return new DepositCreditDecision(DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF, policy.version());
    }

    private static DepositCreditDecision denied(DepositCreditDecisionReason reason, DepositCreditAssetNetworkPolicy policy) {
        return new DepositCreditDecision(reason, policy.version());
    }
}
