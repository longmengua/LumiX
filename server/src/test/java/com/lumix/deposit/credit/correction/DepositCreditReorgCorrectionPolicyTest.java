package com.lumix.deposit.credit.correction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.deposit.address.DepositAddress;
import com.lumix.deposit.address.DepositAddressFormat;
import com.lumix.deposit.address.DepositAddressLifecycle;
import com.lumix.deposit.address.DepositAddressOwnership;
import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.credit.DepositCreditCandidate;
import com.lumix.deposit.credit.DepositCreditDecision;
import com.lumix.deposit.credit.DepositCreditDecisionReason;
import com.lumix.deposit.credit.DepositCreditDecisionRecord;
import com.lumix.deposit.credit.DepositCreditIdempotencyPolicy;
import com.lumix.deposit.credit.DepositCreditPolicyVersion;
import com.lumix.deposit.observation.ChainBlockReference;
import com.lumix.deposit.observation.ChainEventIndex;
import com.lumix.deposit.observation.ChainReferenceId;
import com.lumix.deposit.observation.DepositAtomicAmount;
import com.lumix.deposit.observation.DepositChainObservation;
import com.lumix.deposit.observation.DepositObservationIdentity;
import com.lumix.deposit.observation.ObservationFinality;
import com.lumix.deposit.observation.finality.DepositObservationFinalityState;
import com.lumix.deposit.observation.finality.DepositObservationLifecycle;
import com.lumix.deposit.observation.reconciliation.P23DepositHandoffCandidate;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DepositCreditReorgCorrectionPolicyTest {

    private final DepositCreditReorgCorrectionPolicy policy = new DepositCreditReorgCorrectionPolicy();

    @Test
    void orphanedConfirmedCreditRequiresStrictlyLaterAppendOnlyReversal() {
        DepositCreditDecisionRecord original = originalDecision();
        DepositObservationFinalityState orphaned = new DepositObservationFinalityState(
                original.candidate().observationEvidence().observation().identity(),
                new ChainBlockReference(100, new ChainReferenceId("reorged-block")), 0, DepositObservationLifecycle.ORPHANED);

        DepositCreditReorgCorrectionPlan plan = policy.evaluate(new DepositCreditReorgCorrectionRequest(original, orphaned,
                DepositCreditAppendState.CREDIT_APPEND_CONFIRMED, Optional.of(new AppendOnlyCorrectionOrder(41, 42))));

        assertEquals(DepositCreditReorgDecision.APPEND_ONLY_REVERSAL_REQUIRED, plan.decision());
        assertEquals(42, plan.requiredOrder().orElseThrow().reversalAppendSequence());
        assertThrows(IllegalArgumentException.class, () -> new AppendOnlyCorrectionOrder(42, 42));
    }

    @Test
    void notAppendedCreditFreezesAndUnknownOrMissingEvidenceEscalatesHuman() {
        DepositCreditDecisionRecord original = originalDecision();
        DepositObservationFinalityState orphaned = new DepositObservationFinalityState(
                original.candidate().observationEvidence().observation().identity(),
                new ChainBlockReference(100, new ChainReferenceId("reorged-block")), 0, DepositObservationLifecycle.ORPHANED);

        assertEquals(DepositCreditReorgDecision.FREEZE_PENDING_CREDIT, policy.evaluate(new DepositCreditReorgCorrectionRequest(
                original, orphaned, DepositCreditAppendState.NOT_APPENDED, Optional.empty())).decision());
        assertEquals(DepositCreditReorgDecision.ESCALATE_HUMAN, policy.evaluate(new DepositCreditReorgCorrectionRequest(
                original, orphaned, DepositCreditAppendState.UNKNOWN, Optional.empty())).decision());
        assertEquals(DepositCreditReorgDecision.ESCALATE_HUMAN, policy.evaluate(new DepositCreditReorgCorrectionRequest(
                original, new DepositObservationFinalityState(orphaned.identity(), orphaned.block(), 6,
                DepositObservationLifecycle.FINALITY_THRESHOLD_MET), DepositCreditAppendState.NOT_APPENDED, Optional.empty())).decision());
    }

    private static DepositCreditDecisionRecord originalDecision() {
        DepositNetwork network = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);
        DepositCreditPolicyVersion version = new DepositCreditPolicyVersion("deposit-policy-v1");
        DepositAddress address = DepositAddress.from("0xabcdef0000000000000000000000000000000000", network);
        DepositChainObservation observation = new DepositChainObservation(
                new DepositObservationIdentity(network, new ChainReferenceId("tx-1"), new ChainEventIndex(0)),
                new ChainBlockReference(100, new ChainReferenceId("block-100")), address, new AssetSymbol("USDT"),
                new DepositAtomicAmount(BigInteger.TEN), new ObservationFinality(6, true));
        DepositCreditCandidate candidate = new DepositCreditCandidate(new P23DepositHandoffCandidate(observation,
                new DepositObservationFinalityState(observation.identity(), observation.block(), 6,
                        DepositObservationLifecycle.FINALITY_THRESHOLD_MET)),
                new DepositAddressOwnership(new UserId("user-a"), new AssetSymbol("USDT"), network, address,
                        DepositAddressLifecycle.ACTIVE), version);
        return new DepositCreditIdempotencyPolicy().record(candidate,
                new DepositCreditDecision(DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF, version));
    }
}
