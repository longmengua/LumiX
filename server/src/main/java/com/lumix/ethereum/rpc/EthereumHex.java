package com.lumix.ethereum.rpc;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Ethereum hash 與 address 的 canonical 比對工具；不處理 secret。 */
public final class EthereumHex {
    private static final Pattern HASH = Pattern.compile("0x[0-9a-f]{64}");
    private EthereumHex() { }
    public static String requireHash(String value, String label) {
        String normalized = Objects.requireNonNull(value, label + " must not be null").trim().toLowerCase(Locale.ROOT);
        if (!HASH.matcher(normalized).matches()) throw new IllegalArgumentException(label + " must be a 32-byte hexadecimal hash");
        return normalized;
    }
}
