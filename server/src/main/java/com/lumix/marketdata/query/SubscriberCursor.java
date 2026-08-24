package com.lumix.marketdata.query;

import com.lumix.marketdata.contract.StreamKey;
import java.util.Objects;
import java.util.OptionalLong;

/** subscriber 的 local cursor；resnapshot 後 cursor 必須清空，不能保留舊 version 假裝連續。 */
public record SubscriberCursor(StreamKey streamKey, OptionalLong lastDeliveredVersion, int pendingUpdates) {

    public SubscriberCursor {
        streamKey = Objects.requireNonNull(streamKey, "streamKey must not be null");
        lastDeliveredVersion = Objects.requireNonNull(lastDeliveredVersion, "lastDeliveredVersion must not be null");
        if (lastDeliveredVersion.isPresent() && lastDeliveredVersion.getAsLong() < 1) {
            throw new IllegalArgumentException("subscriber version must be positive");
        }
        if (pendingUpdates < 0) {
            throw new IllegalArgumentException("pendingUpdates must not be negative");
        }
    }

    public static SubscriberCursor fresh(StreamKey streamKey) {
        return new SubscriberCursor(streamKey, OptionalLong.empty(), 0);
    }
}
