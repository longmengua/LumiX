package com.lumix.wallet;

import com.lumix.account.UserId;

import java.util.List;

/**
 * 充值服務契約。
 */
public interface DepositService {

    // TODO: requires high-reasoning review before production use
    DepositRecord registerObservedDeposit(DepositRecord depositRecord);

    // TODO: requires high-reasoning review before production use
    List<DepositRecord> listDeposits(UserId userId);
}
