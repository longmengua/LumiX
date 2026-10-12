package com.lumix.withdrawal.signing;

import java.math.BigInteger;
import java.time.Instant;

/** 主 server 到實體 signer 的 capability boundary；不暴露 key、mnemonic 或 HSM material。 */
public interface SignerGateway {
    SigningResult sign(SigningRequest request);
    /** HTTP timeout 後的 durable signer result lookup；null 代表尚未簽署。 */
    SigningResult result(String withdrawalId,String intentHash);
    record SigningRequest(String withdrawalId,String intentHash,long chainId,String asset,String tokenContract,String from,String to,BigInteger amount,BigInteger amountAtomic,BigInteger nonce,BigInteger gasLimit,BigInteger maxFeePerGas,BigInteger maxPriorityFeePerGas,String data,Instant expiresAt) { }
    record SigningResult(String transactionHash,String signedRawTransaction,boolean replayed) { }
}
