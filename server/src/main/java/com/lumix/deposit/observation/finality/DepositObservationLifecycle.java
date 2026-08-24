package com.lumix.deposit.observation.finality;

/**
 * 入金候選觀測的非帳務生命週期。
 */
public enum DepositObservationLifecycle {
    PENDING_CONFIRMATION,
    FINALITY_THRESHOLD_MET,
    ORPHANED,
    QUARANTINED
}
