package com.lumix.deposit.observation.finality;

/**
 * network/asset policy 在此層已決定的最低確認數。
 *
 * <p>此值只用於分類觀測 finality，不能單獨作為 credit 或資產可用性的依據。</p>
 */
public record RequiredConfirmations(long value) {

    public RequiredConfirmations {
        if (value <= 0) {
            throw new IllegalArgumentException("required confirmations must be positive");
        }
    }
}
