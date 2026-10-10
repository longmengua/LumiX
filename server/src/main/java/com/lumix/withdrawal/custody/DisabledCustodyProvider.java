package com.lumix.withdrawal.custody;

import org.springframework.stereotype.Component;

/** 預設 custody provider；避免未設定受管 provider 時意外簽章、廣播或移動資金。 */
@Component
public final class DisabledCustodyProvider implements CustodyProvider {
    @Override public CustodyWithdrawalResult createWithdrawal(CustodyWithdrawalRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return new CustodyWithdrawalResult(CustodyWithdrawalStatus.REJECTED_DISABLED, java.util.Optional.empty());
    }
}
