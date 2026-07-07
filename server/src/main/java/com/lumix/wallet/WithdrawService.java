package com.lumix.wallet;

import com.lumix.account.UserId;

import java.util.List;

/**
 * 提現服務契約。
 */
public interface WithdrawService {

    // TODO(HUMAN_REVIEW_REQUIRED): 提現申請只定義契約；正式簽章與廣播流程留待後續階段。
    WithdrawRecord submitWithdrawal(WithdrawRecord withdrawRecord);

    // 查詢使用者提現紀錄，供營運與對帳查看。
    List<WithdrawRecord> listWithdrawals(UserId userId);
}
