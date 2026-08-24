package com.lumix.withdrawal.broadcast;

import java.util.List;
import java.util.Objects;

/** 唯讀對帳報告；只有 confirmed evidence 完整時才標示可供未來 completion handoff 檢查。 */
public record WithdrawalBroadcastReconciliationReport(List<WithdrawalBroadcastReconciliationException> exceptions, boolean confirmedForFutureHandoff) {
    public WithdrawalBroadcastReconciliationReport { exceptions = List.copyOf(Objects.requireNonNull(exceptions, "exceptions")); if (!exceptions.isEmpty() && confirmedForFutureHandoff) throw new IllegalArgumentException("exception report cannot be confirmed"); }
}
