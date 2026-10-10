package com.lumix.ethereum.scanner;

import com.lumix.ethereum.asset.EthereumAssetConfig;
import com.lumix.ethereum.asset.EthereumAssetRegistry;
import com.lumix.ethereum.rpc.EthereumHex;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** 官方 USDT Transfer event 的已驗證 domain evidence；不接受由外部輸入指定的 contract。 */
public record EthereumUsdtTransferLog(
        String transactionHash,
        long logIndex,
        BigInteger blockNumber,
        String blockHash,
        String fromAddress,
        String toAddress,
        BigInteger amountRaw
) {
    /** ERC-20 Transfer(address,address,uint256) 的固定 keccak topic，不允許前端或設定覆寫。 */
    public static final String TRANSFER_TOPIC = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    public EthereumUsdtTransferLog {
        transactionHash = EthereumHex.requireHash(transactionHash, "transaction hash");
        blockNumber = Objects.requireNonNull(blockNumber, "blockNumber must not be null");
        blockHash = EthereumHex.requireHash(blockHash, "block hash");
        fromAddress = EthereumAssetConfig.normalizeAddress(fromAddress);
        toAddress = EthereumAssetConfig.normalizeAddress(toAddress);
        amountRaw = Objects.requireNonNull(amountRaw, "amountRaw must not be null");
        if (logIndex < 0 || blockNumber.signum() < 0 || amountRaw.signum() <= 0) {
            throw new IllegalArgumentException("USDT transfer evidence has invalid numeric values");
        }
    }

    /** 將原始 log 轉為受限 evidence；非官方合約、topic 或 ABI 長度一律拒絕。 */
    public static EthereumUsdtTransferLog fromRaw(
            String contractAddress, String transactionHash, long logIndex, BigInteger blockNumber, String blockHash,
            List<String> topics, String data
    ) {
        String normalizedContract = EthereumAssetConfig.normalizeAddress(contractAddress);
        if (!EthereumAssetRegistry.USDT_CONTRACT.equals(normalizedContract)) {
            throw new IllegalArgumentException("only the official Ethereum USDT contract is supported");
        }
        List<String> eventTopics = List.copyOf(Objects.requireNonNull(topics, "topics must not be null"));
        if (eventTopics.size() != 3 || !TRANSFER_TOPIC.equals(normalizeTopic(eventTopics.getFirst()))) {
            throw new IllegalArgumentException("log is not an ERC-20 Transfer event");
        }
        return new EthereumUsdtTransferLog(transactionHash, logIndex, blockNumber, blockHash,
                topicAddress(eventTopics.get(1)), topicAddress(eventTopics.get(2)), uint256(data));
    }

    private static String topicAddress(String topic) {
        String normalized = normalizeTopic(topic);
        return EthereumAssetConfig.normalizeAddress("0x" + normalized.substring(26));
    }

    private static BigInteger uint256(String data) {
        String normalized = normalizeTopic(data);
        return new BigInteger(normalized.substring(2), 16);
    }

    private static String normalizeTopic(String value) {
        String normalized = Objects.requireNonNull(value, "topic must not be null").trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("0x[0-9a-f]{64}")) throw new IllegalArgumentException("event word must be 32-byte hexadecimal");
        return normalized;
    }
}
