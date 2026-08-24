package com.lumix.withdrawal.broadcast;

import com.lumix.withdrawal.signing.WithdrawalSigningIntent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** P25-T04 的 read-only evidence validator；不 retry、不呼叫 broadcaster，也不建立 ledger/balance/reservation handoff。 */
public final class WithdrawalBroadcastReconciliationPolicy {
    public WithdrawalBroadcastReconciliationReport evaluate(WithdrawalSigningIntent intent, List<WithdrawalBroadcastEvidence> evidence) {
        intent = Objects.requireNonNull(intent, "intent"); evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        List<WithdrawalBroadcastReconciliationException> exceptions = new ArrayList<>(); Set<String> attempts = new HashSet<>(); boolean terminalSeen = false; boolean confirmed = false;
        for (WithdrawalBroadcastEvidence item : evidence) {
            if (!intent.intentDigest().equals(item.signingIntentDigest())) exceptions.add(WithdrawalBroadcastReconciliationException.INTENT_DIGEST_MISMATCH);
            if (!attempts.add(item.attemptId())) exceptions.add(WithdrawalBroadcastReconciliationException.DUPLICATE_ATTEMPT_ID);
            if (terminalSeen) exceptions.add(WithdrawalBroadcastReconciliationException.TERMINAL_STATE_REGRESSION);
            if (item.status() == WithdrawalBroadcastStatus.CONFIRMED) { terminalSeen = true; confirmed = true; }
            if (item.status() == WithdrawalBroadcastStatus.FAILED) terminalSeen = true;
        }
        if (!confirmed) exceptions.add(WithdrawalBroadcastReconciliationException.CONFIRMATION_NOT_FINAL);
        return new WithdrawalBroadcastReconciliationReport(exceptions, exceptions.isEmpty());
    }
}
