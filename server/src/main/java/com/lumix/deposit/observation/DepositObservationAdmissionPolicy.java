package com.lumix.deposit.observation;

import com.lumix.deposit.address.DepositNetwork;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * P22-T02 的 pure、deterministic observation admission policy。
 *
 * <p>它不修改 caller 的資料、不呼叫 provider；僅在完整相同的重播下忽略 duplicate。identity 相同但內容不同、
 * network 不符或游標倒退時一律拒絕，避免觀測缺口被靜默掩蓋。</p>
 */
public final class DepositObservationAdmissionPolicy {

    /**
     * 驗證並 canonicalize 一批同 network 的觀測。known 需來自既有 immutable evidence，不能是未驗證快取。
     */
    public DepositObservationEvaluation evaluate(
            DepositNetwork network,
            Optional<DepositObservationCursor> afterExclusive,
            Map<DepositObservationIdentity, DepositChainObservation> known,
            Collection<DepositChainObservation> candidates
    ) {
        DepositNetwork requestedNetwork = Objects.requireNonNull(network, "network must not be null");
        Optional<DepositObservationCursor> persistedCursor = Objects.requireNonNull(
                afterExclusive, "afterExclusive must not be null");
        Map<DepositObservationIdentity, DepositChainObservation> knownObservations = Map.copyOf(
                Objects.requireNonNull(known, "known must not be null"));
        List<DepositChainObservation> suppliedCandidates = List.copyOf(
                Objects.requireNonNull(candidates, "candidates must not be null"));
        persistedCursor.ifPresent(cursor -> requireNetwork(requestedNetwork, cursor.network(), "cursor"));

        List<DepositChainObservation> ordered = suppliedCandidates.stream()
                .peek(candidate -> requireNetwork(requestedNetwork, candidate.network(), "candidate"))
                .sorted(Comparator.comparing(DepositChainObservation::cursor))
                .toList();
        Map<DepositObservationIdentity, DepositChainObservation> seenInBatch = new LinkedHashMap<>();
        List<DepositChainObservation> accepted = new ArrayList<>();
        List<DepositChainObservation> duplicateIgnored = new ArrayList<>();

        for (DepositChainObservation candidate : ordered) {
            requireAfterCursor(persistedCursor, candidate.cursor());
            DepositChainObservation knownValue = knownObservations.get(candidate.identity());
            DepositChainObservation priorInBatch = seenInBatch.putIfAbsent(candidate.identity(), candidate);
            if (knownValue != null) {
                requireIdentical(candidate, knownValue);
                duplicateIgnored.add(candidate);
            } else if (priorInBatch != null) {
                requireIdentical(candidate, priorInBatch);
                duplicateIgnored.add(candidate);
            } else {
                accepted.add(candidate);
            }
        }

        Optional<DepositObservationCursor> nextCursor = accepted.isEmpty()
                ? persistedCursor
                : Optional.of(accepted.getLast().cursor());
        return new DepositObservationEvaluation(accepted, duplicateIgnored, nextCursor);
    }

    private static void requireNetwork(DepositNetwork expected, DepositNetwork actual, String valueName) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(valueName + " network does not match requested network");
        }
    }

    private static void requireAfterCursor(Optional<DepositObservationCursor> afterExclusive, DepositObservationCursor candidate) {
        if (afterExclusive.isPresent() && candidate.compareTo(afterExclusive.orElseThrow()) <= 0) {
            throw new IllegalArgumentException("candidate cursor must advance beyond the persisted cursor");
        }
    }

    private static void requireIdentical(DepositChainObservation candidate, DepositChainObservation existing) {
        if (!candidate.equals(existing)) {
            throw new IllegalArgumentException("observation identity collision has conflicting evidence");
        }
    }
}
