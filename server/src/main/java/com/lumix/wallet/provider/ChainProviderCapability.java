package com.lumix.wallet.provider;

/** 可選 provider capability；未宣告即不可呼叫，避免 registry 假設所有供應商功能相同。 */
public enum ChainProviderCapability {
    DEPOSIT_OBSERVATION,
    ADDRESS_VALIDATION,
    ADDRESS_ALLOCATION,
    TRANSACTION_BROADCAST,
    TRANSACTION_LOOKUP
}
