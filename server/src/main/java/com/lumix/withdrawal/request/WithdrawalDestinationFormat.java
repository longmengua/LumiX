package com.lumix.withdrawal.request;
/** 提款 destination 的明確編碼格式；不可根據字串猜測 chain。 */
public enum WithdrawalDestinationFormat { EVM_HEX, BASE58, BECH32 }
