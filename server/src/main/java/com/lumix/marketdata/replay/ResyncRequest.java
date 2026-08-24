package com.lumix.marketdata.replay;

import com.lumix.marketdata.contract.StreamKey;
import java.time.Instant;
import java.util.Objects;

/**
 * 唯讀 recovery 邊界輸出。沒有 endpoint、credential、retry timer 或 side effect，避免 replay 偷變成 provider runtime。
 */
public record ResyncRequest(StreamKey streamKey, ResyncReason reason, Instant detectedAtSourceTimestamp) {

    public ResyncRequest {
        streamKey = Objects.requireNonNull(streamKey, "streamKey must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null");
        detectedAtSourceTimestamp = Objects.requireNonNull(detectedAtSourceTimestamp, "detectedAtSourceTimestamp must not be null");
    }
}
