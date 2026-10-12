package com.lumix.ethereum.rpc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Ethereum provider-neutral 唯讀 RPC boundary。
 *
 * <p>介面刻意不提供 transaction signing 或 broadcasting；Alchemy、Infura、QuickNode 與自建節點只能在
 * infrastructure adapter 實作這些觀測方法。</p>
 */
public interface EthereumRpcClient {
    EthereumBlock finalizedBlock() throws IOException;
    EthereumBlock blockByNumber(BigInteger number) throws IOException;
    /** pending nonce 供 allocator 初始化；不可用 latest nonce 替代。 */
    default BigInteger pendingTransactionCount(String address) throws IOException { throw new UnsupportedOperationException("pending nonce lookup is unavailable"); }
    /** signed raw transaction 的唯一外送邊界；signer 絕不呼叫此方法。 */
    default String sendRawTransaction(String signedRawTransaction) throws IOException { throw new UnsupportedOperationException("transaction broadcasting is unavailable"); }
    /** RPC timeout 時用 tx hash 對帳，不得直接重新簽章。 */
    default Optional<EthereumTransactionReceipt> transactionReceipt(String transactionHash) throws IOException { throw new UnsupportedOperationException("receipt lookup is unavailable"); }
}
