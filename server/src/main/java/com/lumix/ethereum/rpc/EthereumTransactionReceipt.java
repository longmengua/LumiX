package com.lumix.ethereum.rpc;

import java.math.BigInteger;

/** 去除 provider 細節後的交易收據；SUCCESS 前不可做提款帳本結算。 */
public record EthereumTransactionReceipt(
    String transactionHash,
    BigInteger blockNumber,
    String blockHash,
    boolean success,
    BigInteger gasUsed,
    BigInteger effectiveGasPrice
) { }
