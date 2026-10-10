package com.lumix.ethereum.finality;

import com.lumix.ethereum.rpc.EthereumBlock;
import java.math.BigInteger;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 集中執行 Ethereum PoS finalized 判定。
 *
 * <p>這裡絕不以 latest block 減高度作為 credit 條件：只有 deposit 所在 canonical block hash 保持一致，
 * 且高度不高於 eth_getBlockByNumber(finalized) 的高度時，才回傳 FINALIZED。</p>
 */
@Service
public final class EthereumFinalityService {
    public EthereumFinalityDecision assess(EthereumBlock depositBlock, EthereumBlock canonicalDepositBlock, EthereumBlock finalizedBlock) {
        EthereumBlock observed = Objects.requireNonNull(depositBlock, "depositBlock must not be null");
        EthereumBlock canonical = Objects.requireNonNull(canonicalDepositBlock, "canonicalDepositBlock must not be null");
        EthereumBlock finalized = Objects.requireNonNull(finalizedBlock, "finalizedBlock must not be null");
        if (!observed.number().equals(canonical.number()) || !observed.hash().equals(canonical.hash())) return EthereumFinalityDecision.REORGED;
        return observed.number().compareTo(finalized.number()) <= 0
            ? EthereumFinalityDecision.FINALIZED
            : EthereumFinalityDecision.CONFIRMING;
    }

    public boolean isFinalizedAtOrBefore(BigInteger depositBlockNumber, BigInteger finalizedBlockNumber) {
        return Objects.requireNonNull(depositBlockNumber, "depositBlockNumber must not be null")
            .compareTo(Objects.requireNonNull(finalizedBlockNumber, "finalizedBlockNumber must not be null")) <= 0;
    }
}
