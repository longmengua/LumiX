package com.lumix.signer;

/** 同 withdrawal id 與 intent hash 重送時回傳完全相同的 signed transaction。 */
public record SignerResult(String transactionHash,String signedRawTransaction,boolean replayed) { }
