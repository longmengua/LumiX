package com.lumix.deposit.credit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.deposit.address.DepositAddress;
import com.lumix.deposit.address.DepositAddressFormat;
import com.lumix.deposit.address.DepositAddressLifecycle;
import com.lumix.deposit.address.DepositAddressOwnership;
import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.observation.ChainBlockReference;
import com.lumix.deposit.observation.ChainEventIndex;
import com.lumix.deposit.observation.ChainReferenceId;
import com.lumix.deposit.observation.DepositAtomicAmount;
import com.lumix.deposit.observation.DepositChainObservation;
import com.lumix.deposit.observation.DepositObservationIdentity;
import com.lumix.deposit.observation.ObservationFinality;
import com.lumix.deposit.observation.finality.DepositObservationFinalityState;
import com.lumix.deposit.observation.finality.DepositObservationLifecycle;
import com.lumix.deposit.observation.finality.RequiredConfirmations;
import com.lumix.deposit.observation.reconciliation.P23DepositHandoffCandidate;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class DepositCreditEligibilityPolicyTest {

    private final DepositCreditEligibilityPolicy policy = new DepositCreditEligibilityPolicy();
    private final DepositNetwork network = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);
    private final DepositCreditPolicyVersion version = new DepositCreditPolicyVersion("deposit-policy-v1");

    @Test
    void insufficientConfirmationWrongAssetNetworkAndVersionMismatchFailClosed() {
        DepositCreditCandidate candidate = candidate(network, "USDT", 5, version);
        DepositCreditAssetNetworkPolicy requiredSix = policy(network, "USDT", 6, version);

        assertEquals(DepositCreditDecisionReason.INSUFFICIENT_CONFIRMATIONS, policy.evaluate(candidate, requiredSix).reason());
        assertEquals(DepositCreditDecisionReason.ASSET_MISMATCH, policy.evaluate(candidate, policy(network, "USDC", 5, version)).reason());
        DepositNetwork otherNetwork = new DepositNetwork("BSC_MAINNET", DepositAddressFormat.EVM_HEX);
        assertEquals(DepositCreditDecisionReason.NETWORK_MISMATCH, policy.evaluate(candidate, policy(otherNetwork, "USDT", 5, version)).reason());
        assertEquals(DepositCreditDecisionReason.POLICY_VERSION_MISMATCH,
                policy.evaluate(candidate(network, "USDT", 5, new DepositCreditPolicyVersion("deposit-policy-v0")), policy(network, "USDT", 5, version)).reason());
    }

    @Test
    void matchingFinalityOwnershipAssetNetworkAndVersionIsOnlyFutureHandoffEligible() {
        DepositCreditDecision decision = policy.evaluate(candidate(network, "USDT", 6, version), policy(network, "USDT", 6, version));

        assertEquals(DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF, decision.reason());
    }

    private DepositCreditCandidate candidate(DepositNetwork candidateNetwork, String asset, long confirmations, DepositCreditPolicyVersion version) {
        DepositAddress address = DepositAddress.from("0xabcdef0000000000000000000000000000000000", candidateNetwork);
        DepositChainObservation observation = new DepositChainObservation(
                new DepositObservationIdentity(candidateNetwork, new ChainReferenceId("tx-1"), new ChainEventIndex(0)),
                new ChainBlockReference(100, new ChainReferenceId("block-100")), address, new AssetSymbol(asset),
                new DepositAtomicAmount(BigInteger.TEN), new ObservationFinality(confirmations, true));
        return new DepositCreditCandidate(
                new P23DepositHandoffCandidate(observation, new DepositObservationFinalityState(
                        observation.identity(), observation.block(), confirmations, DepositObservationLifecycle.FINALITY_THRESHOLD_MET)),
                new DepositAddressOwnership(new UserId("user-a"), new AssetSymbol(asset), candidateNetwork, address, DepositAddressLifecycle.ACTIVE), version);
    }

    private DepositCreditAssetNetworkPolicy policy(
            DepositNetwork policyNetwork, String asset, long confirmations, DepositCreditPolicyVersion policyVersion
    ) {
        return new DepositCreditAssetNetworkPolicy(policyVersion, policyNetwork, new AssetSymbol(asset),
                new RequiredConfirmations(confirmations), true);
    }
}
