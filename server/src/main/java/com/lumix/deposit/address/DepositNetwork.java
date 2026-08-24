package com.lumix.deposit.address;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * provider-neutral chain/network identity。
 *
 * <p>network 是入金事件 identity 的一部分；同一 address 文字在不同 network 絕不能被當成相同所有權。</p>
 */
public record DepositNetwork(String code, DepositAddressFormat addressFormat) {

    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,31}");

    public DepositNetwork {
        code = Objects.requireNonNull(code, "code must not be null").trim().toUpperCase(Locale.ROOT);
        addressFormat = Objects.requireNonNull(addressFormat, "addressFormat must not be null");
        if (!CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("deposit network code must be canonical uppercase identifier");
        }
    }
}
