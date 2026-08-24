package com.lumix.deposit.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AssetSymbol;
import com.lumix.deposit.address.DepositAddress;
import com.lumix.deposit.address.DepositAddressFormat;
import com.lumix.deposit.address.DepositNetwork;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DepositObservationAdmissionPolicyTest {

    private final DepositObservationAdmissionPolicy policy = new DepositObservationAdmissionPolicy();
    private final DepositNetwork ethereum = new DepositNetwork("ETH_MAINNET", DepositAddressFormat.EVM_HEX);

    @Test
    void identicalKnownObservationIsIgnoredButConflictingIdentityFailsClosed() {
        DepositChainObservation observation = observation(ethereum, 20, "block-20", "tx-01", 0, "100");

        DepositObservationEvaluation replay = policy.evaluate(
                ethereum, Optional.empty(), Map.of(observation.identity(), observation), List.of(observation));

        assertEquals(List.of(), replay.accepted());
        assertEquals(List.of(observation), replay.duplicateIgnored());
        assertEquals(Optional.empty(), replay.nextCursor());
        assertThrows(IllegalArgumentException.class, () -> policy.evaluate(
                ethereum,
                Optional.empty(),
                Map.of(observation.identity(), observation),
                List.of(observation(ethereum, 20, "block-20", "tx-01", 0, "101"))));
    }

    @Test
    void wrongNetworkAndCursorRegressionAreRejectedBeforeAnyCreditBoundary() {
        DepositNetwork bitcoin = new DepositNetwork("BTC_MAINNET", DepositAddressFormat.BECH32);
        DepositChainObservation ethereumObservation = observation(ethereum, 30, "block-30", "tx-30", 0, "10");
        DepositChainObservation older = observation(ethereum, 29, "block-29", "tx-29", 0, "10");

        assertThrows(IllegalArgumentException.class, () -> policy.evaluate(
                bitcoin, Optional.empty(), Map.of(), List.of(ethereumObservation)));
        assertThrows(IllegalArgumentException.class, () -> policy.evaluate(
                ethereum, Optional.of(ethereumObservation.cursor()), Map.of(), List.of(older)));
        assertThrows(IllegalArgumentException.class, () -> new DepositChainObservation(
                new DepositObservationIdentity(ethereum, new ChainReferenceId("tx-bad-address"), new ChainEventIndex(0)),
                new ChainBlockReference(31, new ChainReferenceId("block-31")),
                DepositAddress.from("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080", bitcoin),
                new AssetSymbol("BTC"),
                new DepositAtomicAmount(BigInteger.ONE),
                new ObservationFinality(0, false)));
    }

    @Test
    void replayOrderIsCanonicalRegardlessOfProviderInputOrder() {
        DepositChainObservation later = observation(ethereum, 41, "block-41", "tx-b", 1, "2");
        DepositChainObservation first = observation(ethereum, 40, "block-40", "tx-z", 3, "1");
        DepositChainObservation second = observation(ethereum, 41, "block-41", "tx-a", 0, "3");

        DepositObservationEvaluation evaluation = policy.evaluate(
                ethereum, Optional.empty(), Map.of(), List.of(later, first, second, later));

        assertEquals(List.of(first, second, later), evaluation.accepted());
        assertEquals(List.of(later), evaluation.duplicateIgnored());
        assertEquals(Optional.of(later.cursor()), evaluation.nextCursor());
    }

    private static DepositChainObservation observation(
            DepositNetwork network,
            long blockHeight,
            String blockHash,
            String transactionId,
            long eventIndex,
            String amount
    ) {
        return new DepositChainObservation(
                new DepositObservationIdentity(network, new ChainReferenceId(transactionId), new ChainEventIndex(eventIndex)),
                new ChainBlockReference(blockHeight, new ChainReferenceId(blockHash)),
                DepositAddress.from("0xabcdef0000000000000000000000000000000000", network),
                new AssetSymbol("USDT"),
                new DepositAtomicAmount(new BigInteger(amount)),
                new ObservationFinality(0, false));
    }
}
