package com.lumix.deposit.credit;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 可審計的 credit policy version；版本不符時不得以最新規則靜默覆蓋舊 evidence。
 */
public record DepositCreditPolicyVersion(String value) {

    private static final Pattern CANONICAL = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public DepositCreditPolicyVersion {
        value = Objects.requireNonNull(value, "value must not be null");
        if (!CANONICAL.matcher(value).matches()) {
            throw new IllegalArgumentException("policy version must be a canonical non-blank identifier");
        }
    }
}
