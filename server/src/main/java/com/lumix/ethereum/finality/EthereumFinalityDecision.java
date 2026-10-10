package com.lumix.ethereum.finality;

/** finalized checkpoint 對單筆 deposit evidence 的唯一判定結果。 */
public enum EthereumFinalityDecision { CONFIRMING, FINALIZED, REORGED }
