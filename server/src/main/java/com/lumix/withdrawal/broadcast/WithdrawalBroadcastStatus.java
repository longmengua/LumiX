package com.lumix.withdrawal.broadcast;

/** 外部 broadcaster 回傳的唯讀狀態；FAILED 不表示可自動釋放 hold 或重送資金。 */
public enum WithdrawalBroadcastStatus { ACCEPTED, PENDING_CONFIRMATION, REPLACED, FAILED, CONFIRMED }
