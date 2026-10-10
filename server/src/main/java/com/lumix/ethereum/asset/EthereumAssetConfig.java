package com.lumix.ethereum.asset;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Ethereum Mainnet 可觀測資產的不可變白名單項目。
 *
 * <p>contract address 只允許服務端常數建立；scanner 絕不接受 browser 或 API 傳入的 token contract。</p>
 */
public record EthereumAssetConfig(String assetCode, EthereumAssetType type, int decimals, Optional<String> contractAddress) {
    private static final Pattern ADDRESS = Pattern.compile("0x[0-9a-f]{40}");

    public EthereumAssetConfig {
        assetCode = Objects.requireNonNull(assetCode, "assetCode must not be null").trim().toUpperCase(Locale.ROOT);
        type = Objects.requireNonNull(type, "type must not be null");
        contractAddress = Objects.requireNonNull(contractAddress, "contractAddress must not be null").map(EthereumAssetConfig::normalizeAddress);
        if (assetCode.isBlank() || decimals < 0 || decimals > 36) throw new IllegalArgumentException("invalid Ethereum asset precision");
        if (type == EthereumAssetType.NATIVE && contractAddress.isPresent()) throw new IllegalArgumentException("native asset must not have a contract");
        if (type == EthereumAssetType.ERC20 && contractAddress.isEmpty()) throw new IllegalArgumentException("ERC20 asset requires a contract");
    }

    public static String normalizeAddress(String address) {
        String normalized = Objects.requireNonNull(address, "address must not be null").trim().toLowerCase(Locale.ROOT);
        if (!ADDRESS.matcher(normalized).matches()) throw new IllegalArgumentException("Ethereum address must be 20-byte hexadecimal");
        return normalized;
    }
}
