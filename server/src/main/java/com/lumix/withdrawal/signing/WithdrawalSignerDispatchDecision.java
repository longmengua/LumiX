package com.lumix.withdrawal.signing;

/** adapter 前的 fail-closed 交接決策；READY 只是可交接 evidence，並非 signer 呼叫。 */
public enum WithdrawalSignerDispatchDecision { READY_FOR_ISOLATED_ADAPTER, CAPABILITY_UNAVAILABLE_REJECTED, NETWORK_UNSUPPORTED_REJECTED }
