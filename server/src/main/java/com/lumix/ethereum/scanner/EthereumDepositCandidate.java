package com.lumix.ethereum.scanner;

import com.lumix.ethereum.asset.EthereumAssetConfig;
import com.lumix.ethereum.asset.EthereumAssetRegistry;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/** scanner 找到且地址所有權已確認的 immutable deposit candidate；這不是 credit command。 */
public record EthereumDepositCandidate(
        String accountId, String assetCode, String transactionHash, long logIndex, BigInteger blockNumber,
        String blockHash, String fromAddress, String toAddress, Optional<String> contractAddress, BigInteger amountRaw
) {
    public EthereumDepositCandidate {
        accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        if (accountId.isBlank()) throw new IllegalArgumentException("accountId must not be blank");
        EthereumAssetConfig asset = new EthereumAssetRegistry().requireAsset(assetCode);
        assetCode = asset.assetCode();
        transactionHash = com.lumix.ethereum.rpc.EthereumHex.requireHash(transactionHash, "transaction hash");
        blockNumber = Objects.requireNonNull(blockNumber, "blockNumber must not be null");
        blockHash = com.lumix.ethereum.rpc.EthereumHex.requireHash(blockHash, "block hash");
        fromAddress = EthereumAssetConfig.normalizeAddress(fromAddress);
        toAddress = EthereumAssetConfig.normalizeAddress(toAddress);
        contractAddress = Objects.requireNonNull(contractAddress, "contractAddress must not be null").map(EthereumAssetConfig::normalizeAddress);
        amountRaw = Objects.requireNonNull(amountRaw, "amountRaw must not be null");
        if (logIndex < 0 || blockNumber.signum() < 0 || amountRaw.signum() <= 0) throw new IllegalArgumentException("invalid deposit candidate amount or position");
        if (asset.contractAddress().equals(contractAddress) == false) throw new IllegalArgumentException("candidate contract does not match asset whitelist");
        if (assetCode.equals("ETH") && logIndex != 0) throw new IllegalArgumentException("direct ETH must use logIndex zero");
    }
}
