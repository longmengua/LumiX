package com.lumix.ethereum.scanner;

import com.lumix.ethereum.asset.EthereumAssetConfig;
import com.lumix.ethereum.rpc.EthereumHex;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/**
 * 已由 RPC adapter 正規化的直接 ETH transaction evidence。
 *
 * <p>Phase 1 只處理 transaction 的 {@code to} 欄位；contract internal transfer 不會被此模型或 scanner
 * 當成已支援，避免將不完整 trace 能力誤認為可入金。</p>
 */
public record EthereumDirectTransfer(
        String transactionHash,
        String fromAddress,
        Optional<String> toAddress,
        BigInteger valueWei,
        BigInteger blockNumber,
        String blockHash
) {
    public EthereumDirectTransfer {
        transactionHash = EthereumHex.requireHash(transactionHash, "transaction hash");
        fromAddress = EthereumAssetConfig.normalizeAddress(fromAddress);
        toAddress = Objects.requireNonNull(toAddress, "toAddress must not be null")
                .map(EthereumAssetConfig::normalizeAddress);
        valueWei = Objects.requireNonNull(valueWei, "valueWei must not be null");
        blockNumber = Objects.requireNonNull(blockNumber, "blockNumber must not be null");
        blockHash = EthereumHex.requireHash(blockHash, "block hash");
        if (valueWei.signum() < 0 || blockNumber.signum() < 0) {
            throw new IllegalArgumentException("Ethereum transfer values must not be negative");
        }
    }
}
