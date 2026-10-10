package com.lumix.ethereum.rpc;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;

/** provider DTO 已在此轉為 domain-safe block evidence，後續服務不直接依賴 Web3j response。 */
public record EthereumBlock(BigInteger number, String hash, Instant timestamp) {
    public EthereumBlock {
        number = Objects.requireNonNull(number, "number must not be null");
        hash = EthereumHex.requireHash(hash, "block hash");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (number.signum() < 0) throw new IllegalArgumentException("block number must not be negative");
    }
}
