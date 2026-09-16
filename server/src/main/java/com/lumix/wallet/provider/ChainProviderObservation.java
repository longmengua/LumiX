package com.lumix.wallet.provider;

import com.lumix.wallet.ChainType;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;

/** provider 傳回的原始鏈上入金事件 identity；數量必須維持 atomic integer，禁止 binary floating point。 */
public record ChainProviderObservation(
        ChainType chainType, String transactionId, long eventIndex, long blockHeight, String blockHash,
        String destinationAddress, String assetSymbol, BigInteger atomicAmount,
        String cursor, Instant observedAt
) {
    public ChainProviderObservation {
        Objects.requireNonNull(chainType, "chainType must not be null");
        transactionId = required(transactionId, "transactionId");
        blockHash = required(blockHash, "blockHash");
        destinationAddress = required(destinationAddress, "destinationAddress");
        assetSymbol = required(assetSymbol, "assetSymbol");
        cursor = required(cursor, "cursor");
        Objects.requireNonNull(atomicAmount, "atomicAmount must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        if (eventIndex < 0 || blockHeight < 0 || atomicAmount.signum() <= 0) throw new IllegalArgumentException("chain observation fields are invalid");
    }
    private static String required(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
