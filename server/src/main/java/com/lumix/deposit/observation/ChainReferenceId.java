package com.lumix.deposit.observation;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * provider 已 canonical 化的區塊或交易識別字串。
 *
 * <p>不同鏈的 hash 編碼不相同，因此本契約不猜測或轉碼；只接受沒有空白、可安全序列化的 opaque identifier。</p>
 */
public record ChainReferenceId(String value) {

    private static final Pattern SAFE_REFERENCE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,255}");

    public ChainReferenceId {
        value = Objects.requireNonNull(value, "value must not be null");
        if (!SAFE_REFERENCE.matcher(value).matches()) {
            throw new IllegalArgumentException("chain reference must be a non-blank canonical opaque identifier");
        }
    }
}
