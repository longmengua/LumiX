package com.lumix.withdrawal.signing;

import com.lumix.withdrawal.approval.WithdrawalApprovalResult;
import com.lumix.withdrawal.request.WithdrawalRequestId;
import com.lumix.withdrawal.request.reconciliation.P25WithdrawalSignerInput;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * P25-T02 的 immutable signing intent policy。
 *
 * 它只對已批准 evidence 做 canonical digest，讓後續 adapter 可檢查 input 是否被替換；不建立交易、
 * 不選 nonce/UTXO，亦不呼叫 signer。
 */
public final class WithdrawalSigningIntentPolicy {

    public WithdrawalSigningIntentResult evaluate(
            P25WithdrawalSignerInput signerInput,
            WithdrawalApprovalResult approvalResult,
            WithdrawalSigningIntentIdempotencyKey idempotencyKey,
            Map<SigningIntentKey, WithdrawalSigningIntent> existingIntents
    ) {
        signerInput = Objects.requireNonNull(signerInput, "signerInput");
        approvalResult = Objects.requireNonNull(approvalResult, "approvalResult");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        existingIntents = Map.copyOf(Objects.requireNonNull(existingIntents, "existingIntents"));
        if (!approvalResult.isApproved()) {
            return new WithdrawalSigningIntentResult(WithdrawalSigningIntentDecision.APPROVAL_NOT_GRANTED_REJECTED, Optional.empty());
        }

        WithdrawalSigningIntent candidate = new WithdrawalSigningIntent(signerInput, idempotencyKey, digest(signerInput));
        SigningIntentKey key = new SigningIntentKey(signerInput.requestState().request().requestId(), idempotencyKey);
        WithdrawalSigningIntent existing = existingIntents.get(key);
        if (existing == null) {
            return new WithdrawalSigningIntentResult(WithdrawalSigningIntentDecision.INTENT_CREATED, Optional.of(candidate));
        }
        if (existing.intentDigest().equals(candidate.intentDigest())) {
            return new WithdrawalSigningIntentResult(WithdrawalSigningIntentDecision.DUPLICATE_REPLAY, Optional.of(existing));
        }
        return new WithdrawalSigningIntentResult(WithdrawalSigningIntentDecision.CONFLICTING_IDEMPOTENCY_PAYLOAD_REJECTED, Optional.empty());
    }

    /** request identity 與所有後續不可被替換的資產欄位都進入 digest，避免只綁定 request id。 */
    private static String digest(P25WithdrawalSignerInput input) {
        var request = input.requestState().request();
        String canonical = String.join("|", request.requestId().value(), input.holdEvidence().holdEvidenceId(), input.auditDigest(),
                request.asset().value(), request.network().code(), request.destination().canonicalValue(), request.amount().atoms().toString());
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the Java runtime", exception);
        }
    }

    /** idempotency scope 必須含 request，避免不同提款共用 client key 時互相污染。 */
    public record SigningIntentKey(WithdrawalRequestId requestId, WithdrawalSigningIntentIdempotencyKey idempotencyKey) {
        public SigningIntentKey {
            requestId = Objects.requireNonNull(requestId, "requestId");
            idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        }
    }
}
