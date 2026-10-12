package com.lumix.withdrawal.broadcast;

import java.math.BigInteger;

/** 已固定但尚未簽章的 EIP-1559 transaction。 */
public record EthereumTransactionDraft(long chainId,String from,String to,BigInteger value,BigInteger amountAtomic,BigInteger nonce,BigInteger gasLimit,BigInteger maxFeePerGas,BigInteger maxPriorityFeePerGas,String data,String asset,String tokenContract) { }
