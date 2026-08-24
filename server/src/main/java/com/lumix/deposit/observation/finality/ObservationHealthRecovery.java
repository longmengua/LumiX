package com.lumix.deposit.observation.finality;

import com.lumix.deposit.address.DepositNetwork;
import com.lumix.deposit.observation.DepositObservationCursor;
import java.time.Instant;
import java.util.Objects;

/**
 * halt 後由上游已驗證流程明確提出的 recovery evidence。
 *
 * <p>本 record 不代表自動復原授權；呼叫端必須在未來獲核准的監控流程中保存證據與操作審計。</p>
 */
public record ObservationHealthRecovery(
        DepositNetwork network,
        DepositObservationCursor verifiedCursor,
        Instant verifiedAt
) {

    public ObservationHealthRecovery {
        network = Objects.requireNonNull(network, "network must not be null");
        verifiedCursor = Objects.requireNonNull(verifiedCursor, "verifiedCursor must not be null");
        verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        if (!network.equals(verifiedCursor.network())) {
            throw new IllegalArgumentException("recovery cursor network must match recovery network");
        }
    }
}
