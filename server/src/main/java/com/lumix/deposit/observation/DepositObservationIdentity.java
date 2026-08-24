package com.lumix.deposit.observation;

import com.lumix.deposit.address.DepositNetwork;
import java.util.Objects;

/**
 * 可跨重播比對的鏈上事件 identity。
 *
 * <p>network、transaction 與 event/log index 缺一不可，避免不同鏈或同交易多事件被誤判為同一筆入金。</p>
 */
public record DepositObservationIdentity(
        DepositNetwork network,
        ChainReferenceId transactionId,
        ChainEventIndex eventIndex
) {

    public DepositObservationIdentity {
        network = Objects.requireNonNull(network, "network must not be null");
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        eventIndex = Objects.requireNonNull(eventIndex, "eventIndex must not be null");
    }
}
