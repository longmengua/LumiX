package com.lumix.withdrawal.custody;

/** 外部 custody／MPC／HSM 的隔離邊界；application 永不持有 raw private key。 */
public interface CustodyProvider { CustodyWithdrawalResult createWithdrawal(CustodyWithdrawalRequest request); }
