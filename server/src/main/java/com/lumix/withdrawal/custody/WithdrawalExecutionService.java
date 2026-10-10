package com.lumix.withdrawal.custody;

import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 提款執行的 fail-closed gate。
 *
 * <p>設定為 false 時 provider 不能被呼叫，避免 deployment 漏配時仍出金。設定為 true 也不代表安全啟用：
 * 仍需由具名且經核准的 custody provider 實作；目前的 DisabledCustodyProvider 仍會拒絕。</p>
 */
@Service
public final class WithdrawalExecutionService {
    private final WithdrawalExecutionProperties properties;
    private final CustodyProvider custodyProvider;

    public WithdrawalExecutionService(WithdrawalExecutionProperties properties, CustodyProvider custodyProvider) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.custodyProvider = Objects.requireNonNull(custodyProvider, "custodyProvider must not be null");
    }

    public CustodyWithdrawalResult execute(CustodyWithdrawalRequest request) {
        if (!properties.isEnabled()) throw new WithdrawalExecutionDisabledException();
        return custodyProvider.createWithdrawal(Objects.requireNonNull(request, "request must not be null"));
    }
}
