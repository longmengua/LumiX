package com.lumix.wallet;

import com.lumix.account.UserId;

import java.util.List;

/**
 * 提現服務契約。
 */
public interface WithdrawService {

    // TODO: requires high-reasoning review before production use
    WithdrawRecord submitWithdrawal(WithdrawRecord withdrawRecord);

    // TODO: requires high-reasoning review before production use
    List<WithdrawRecord> listWithdrawals(UserId userId);
}
