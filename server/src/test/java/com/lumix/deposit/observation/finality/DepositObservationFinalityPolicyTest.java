package com.lumix.deposit.observation.finality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AssetSymbol;
import com.lumix.deposit.address.DepositAddress;
import com.lumix.deposit.address.DepositAddressFormat;
import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.observation.ChainBlockReference;
import com.lumix.deposit.observation.ChainEventIndex;
import com.lumix.deposit.observation.ChainReferenceId;
import com.lumix.deposit.observation.DepositAtomicAmount;
import com.lumix.deposit.observation.DepositChainObservation;
import com.lumix.deposit.observation.DepositObservationIdentity;
import com.lumix.deposit.observation.ObservationFinality;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DepositObservationFinalityPolicyTest {

    private final DepositObservationFinalityPolicy policy = new DepositObservationFinalityPolicy();
    private final DepositNetwork network = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);

    @Test
    void reorgOrphansTheObservationAndConfirmationRegressionQuarantinesIt() {
        DepositChainObservation original = observation("block-a", 4);
        DepositObservationFinalityState previous = policy.evaluate(Optional.empty(), original, new RequiredConfirmations(6)).state();

        DepositObservationFinalityAssessment reorg = policy.evaluate(
                Optional.of(previous), observation("block-b", 5), new RequiredConfirmations(6));
        DepositObservationFinalityAssessment regression = policy.evaluate(
                Optional.of(previous), observation("block-a", 3), new RequiredConfirmations(6));

        assertEquals(DepositObservationLifecycle.ORPHANED, reorg.state().lifecycle());
        assertEquals(ObservationSafetyEvent.REORG_DETECTED, reorg.safetyEvent());
        assertEquals(DepositObservationLifecycle.QUARANTINED, regression.state().lifecycle());
        assertEquals(ObservationSafetyEvent.CONFIRMATION_REGRESSION, regression.safetyEvent());
    }

    @Test
    void requiredConfirmationsOnlyClassifyObservationAndDoNotCreditAnything() {
        DepositObservationFinalityAssessment assessment = policy.evaluate(
                Optional.empty(), observation("block-a", 6), new RequiredConfirmations(6));

        assertEquals(DepositObservationLifecycle.FINALITY_THRESHOLD_MET, assessment.state().lifecycle());
        assertEquals(ObservationSafetyEvent.NONE, assessment.safetyEvent());
    }

    private DepositChainObservation observation(String blockHash, long confirmations) {
        return new DepositChainObservation(
                new DepositObservationIdentity(network, new ChainReferenceId("tx-1"), new ChainEventIndex(0)),
                new ChainBlockReference(100, new ChainReferenceId(blockHash)),
                DepositAddress.from("0xabcdef0000000000000000000000000000000000", network),
                new AssetSymbol("USDT"),
                new DepositAtomicAmount(BigInteger.TEN),
                new ObservationFinality(confirmations, confirmations >= 6));
    }
}
