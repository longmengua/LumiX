package com.lumix.deposit.address;

/** 地址可觀測性 lifecycle；retired/quarantined 不代表刪除舊 ownership，歷史 observation 仍需可追查。 */
public enum DepositAddressLifecycle {
    ACTIVE,
    RETIRED,
    QUARANTINED
}
