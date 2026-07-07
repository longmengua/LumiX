package com.lumix.wallet;

import com.lumix.account.UserId;

import java.util.List;

/**
 * 充值服務契約。
 */
public interface DepositService {

    // TODO(HUMAN_REVIEW_REQUIRED): 只定義充值觀測事件的契約；正式入帳與確認規則要由後續階段補齊。
    DepositRecord registerObservedDeposit(DepositRecord depositRecord);

    // 查詢使用者的充值紀錄，供 UI 與對帳使用。
    List<DepositRecord> listDeposits(UserId userId);
}
