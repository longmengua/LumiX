package com.lumix.deposit.observation;

/**
 * 同一交易內由鏈或 provider 指定的 event/log 位置。
 *
 * <p>transaction hash 不是充分 identity；ERC-20 transfer 等一筆交易可包含多個 event，因此索引不可省略。</p>
 */
public record ChainEventIndex(long value) {

    public ChainEventIndex {
        if (value < 0) {
            throw new IllegalArgumentException("chain event index must not be negative");
        }
    }
}
