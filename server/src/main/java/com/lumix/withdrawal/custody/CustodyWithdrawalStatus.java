package com.lumix.withdrawal.custody;

/** 目前唯一可執行 outcome 是拒絕；不能以 queued 代表已簽章或已廣播。 */
public enum CustodyWithdrawalStatus { REJECTED_DISABLED }
