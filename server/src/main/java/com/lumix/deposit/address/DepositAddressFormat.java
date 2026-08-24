package com.lumix.deposit.address;

/** 支援的地址 canonicalization 規則；network 明確指定 format，禁止憑字串外觀猜測。 */
public enum DepositAddressFormat {
    EVM_HEX,
    BASE58,
    BECH32
}
