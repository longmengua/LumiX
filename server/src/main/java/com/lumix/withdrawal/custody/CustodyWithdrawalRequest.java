package com.lumix.withdrawal.custody;

import com.lumix.withdrawal.signing.WithdrawalSigningIntent;
import java.util.Objects;

/** 已完成上游審核與 intent 固定後，才可交給隔離 custody adapter 的執行請求。 */
public record CustodyWithdrawalRequest(WithdrawalSigningIntent signingIntent) {
    public CustodyWithdrawalRequest { signingIntent = Objects.requireNonNull(signingIntent, "signingIntent must not be null"); }
}
