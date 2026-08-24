package com.lumix.deposit.credit;

import com.lumix.deposit.address.DepositAddressOwnership;
import com.lumix.deposit.observation.reconciliation.P23DepositHandoffCandidate;
import java.util.Objects;

/**
 * P22 evidence 與已知 address ownership 的唯讀組合，尚未構成任何帳務操作。
 */
public record DepositCreditCandidate(
        P23DepositHandoffCandidate observationEvidence,
        DepositAddressOwnership ownership,
        DepositCreditPolicyVersion evidencePolicyVersion
) {

    public DepositCreditCandidate {
        observationEvidence = Objects.requireNonNull(observationEvidence, "observationEvidence must not be null");
        ownership = Objects.requireNonNull(ownership, "ownership must not be null");
        evidencePolicyVersion = Objects.requireNonNull(evidencePolicyVersion, "evidencePolicyVersion must not be null");
    }
}
