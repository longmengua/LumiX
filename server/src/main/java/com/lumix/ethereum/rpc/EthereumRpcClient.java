package com.lumix.ethereum.rpc;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Ethereum provider-neutral 唯讀 RPC boundary。
 *
 * <p>介面刻意不提供 transaction signing 或 broadcasting；Alchemy、Infura、QuickNode 與自建節點只能在
 * infrastructure adapter 實作這些觀測方法。</p>
 */
public interface EthereumRpcClient {
    EthereumBlock finalizedBlock() throws IOException;
    EthereumBlock blockByNumber(BigInteger number) throws IOException;
}
