package com.lumix.deposit.observation;

import java.util.Objects;

/**
 * 鏈上區塊高度與 hash 的共同 identity。
 *
 * <p>高度本身不足以辨識 reorg；保留 hash 讓下一張 reorg task 可以在不依賴本機時間的情況下比對分叉。</p>
 */
public record ChainBlockReference(long height, ChainReferenceId hash) {

    public ChainBlockReference {
        if (height < 0) {
            throw new IllegalArgumentException("block height must not be negative");
        }
        hash = Objects.requireNonNull(hash, "hash must not be null");
    }
}
