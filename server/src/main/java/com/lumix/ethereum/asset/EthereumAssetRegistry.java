package com.lumix.ethereum.asset;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 首發 Ethereum Mainnet 資產白名單。
 *
 * <p>USDT 合約為官方 Ethereum USD₮ ERC-20 address；未登錄合約一律不產生 deposit observation，
 * 以防同名 token 或使用者輸入任意 contract 被誤 credit。</p>
 */
@Component
public final class EthereumAssetRegistry {
    public static final String ETHEREUM_NETWORK = "ETHEREUM";
    public static final long ETHEREUM_MAINNET_CHAIN_ID = 1L;
    public static final String USDT_CONTRACT = "0xdac17f958d2ee523a2206206994597c13d831ec7";

    private final Map<String, EthereumAssetConfig> byAssetCode;
    private final Map<String, EthereumAssetConfig> byContract;

    public EthereumAssetRegistry() {
        List<EthereumAssetConfig> assets = List.of(
            new EthereumAssetConfig("ETH", EthereumAssetType.NATIVE, 18, Optional.empty()),
            new EthereumAssetConfig("USDT", EthereumAssetType.ERC20, 6, Optional.of(USDT_CONTRACT))
        );
        byAssetCode = assets.stream().collect(Collectors.toUnmodifiableMap(EthereumAssetConfig::assetCode, value -> value));
        byContract = assets.stream().flatMap(value -> value.contractAddress().stream().map(contract -> Map.entry(contract, value)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public EthereumAssetConfig requireAsset(String assetCode) {
        String normalized = Objects.requireNonNull(assetCode, "assetCode must not be null").trim().toUpperCase(Locale.ROOT);
        EthereumAssetConfig value = byAssetCode.get(normalized);
        if (value == null) throw new IllegalArgumentException("unsupported Ethereum asset");
        return value;
    }

    public Optional<EthereumAssetConfig> findErc20ByContract(String contractAddress) {
        return Optional.ofNullable(byContract.get(EthereumAssetConfig.normalizeAddress(contractAddress)));
    }
}
