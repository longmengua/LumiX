package com.lumix.withdrawal.request.lifecycle;

/** transition 結果；STALE/INVALID 都不得透過 retry 改寫原狀態。 */
public enum WithdrawalRequestTransitionDecision { TRANSITIONED, IDEMPOTENT_NO_CHANGE, STALE_STATE_REJECTED, INVALID_TRANSITION_REJECTED }
