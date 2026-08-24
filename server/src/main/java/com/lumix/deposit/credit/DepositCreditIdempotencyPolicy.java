package com.lumix.deposit.credit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * P23-T02 的 pure idempotency decision policy。
 *
 * <p>caller 以 immutable existing records 模擬任何 concurrent retry 的觀測結果。本 policy 不做 lookup、lock、寫入或 ledger call；
 * 同 key 異 payload 必須 fail-closed，不能以重試覆蓋舊 decision。</p>
 */
public final class DepositCreditIdempotencyPolicy {

    public DepositCreditDecisionRecord record(DepositCreditCandidate candidate, DepositCreditDecision decision) {
        candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        decision = Objects.requireNonNull(decision, "decision must not be null");
        String keyMaterial = String.join("|",
                candidate.observationEvidence().observation().network().code(),
                candidate.observationEvidence().observation().identity().transactionId().value(),
                Long.toString(candidate.observationEvidence().observation().identity().eventIndex().value()),
                candidate.observationEvidence().observation().asset().value(),
                candidate.ownership().ownerUserId().value(),
                candidate.evidencePolicyVersion().value());
        String payloadMaterial = keyMaterial + "|"
                + candidate.observationEvidence().observation().block().hash().value() + "|"
                + candidate.observationEvidence().observation().amount().atoms() + "|"
                + decision.reason() + "|" + decision.policyVersion().value();
        return new DepositCreditDecisionRecord(new DepositCreditIdempotencyKey(sha256(keyMaterial)), sha256(payloadMaterial), candidate, decision);
    }

    public DepositCreditIdempotencyResult evaluate(
            DepositCreditDecisionRecord candidate,
            Map<DepositCreditIdempotencyKey, DepositCreditDecisionRecord> existing
    ) {
        candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        existing = Map.copyOf(Objects.requireNonNull(existing, "existing must not be null"));
        Optional<DepositCreditDecisionRecord> previous = Optional.ofNullable(existing.get(candidate.idempotencyKey()));
        if (!candidate.decision().eligibleForFutureHandoff()) {
            return new DepositCreditIdempotencyResult(DepositCreditIdempotencyDecision.INELIGIBLE_REJECTED, candidate, previous);
        }
        if (previous.isEmpty()) {
            return new DepositCreditIdempotencyResult(DepositCreditIdempotencyDecision.NEW_RECORD, candidate, Optional.empty());
        }
        if (previous.orElseThrow().payloadFingerprint().equals(candidate.payloadFingerprint())) {
            return new DepositCreditIdempotencyResult(DepositCreditIdempotencyDecision.DUPLICATE_REPLAY,
                    previous.orElseThrow(), previous);
        }
        return new DepositCreditIdempotencyResult(DepositCreditIdempotencyDecision.CONFLICTING_PAYLOAD_REJECTED,
                previous.orElseThrow(), previous);
    }

    private static String sha256(String material) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must exist in the Java runtime", exception);
        }
    }
}
