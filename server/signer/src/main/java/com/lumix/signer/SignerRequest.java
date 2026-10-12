package com.lumix.signer;

import java.math.BigInteger;
import java.time.Instant;

/** Wallet server 送來的固定 signing payload；不含任何 user session、ledger 或 review 資料。 */
public record SignerRequest(String withdrawalId,String intentHash,long chainId,String asset,String tokenContract,String from,String to,BigInteger amount,BigInteger amountAtomic,BigInteger nonce,BigInteger gasLimit,BigInteger maxFeePerGas,BigInteger maxPriorityFeePerGas,String data,Instant expiresAt) { }
