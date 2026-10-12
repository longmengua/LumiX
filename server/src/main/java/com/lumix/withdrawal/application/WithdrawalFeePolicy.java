package com.lumix.withdrawal.application;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** 集中費率邊界。首版未設定平台費，network gas 由 hot wallet 另行負擔，兩者不可混為同一數字。 */
@Service
public class WithdrawalFeePolicy {
    public Quote quote(String asset, String network, BigDecimal amount) { return new Quote(BigDecimal.ZERO, "HOT_WALLET_NETWORK_GAS"); }
    public record Quote(BigDecimal platformFee, String networkGasPolicy) { }
}
