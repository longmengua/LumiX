package com.lumix.marketdata.contract;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 集中保存 contract 的 fail-closed 驗證工具，避免不同 value object 對同一輸入給出不同拒絕語意。
 */
final class MarketDataContractValidation {

    static final Pattern SOURCE_PATTERN = Pattern.compile("[a-z][a-z0-9-]{0,62}");
    static final Pattern INSTRUMENT_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9._:-]{0,63}");
    static final Pattern OPAQUE_IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    static final Pattern PLAIN_DECIMAL_PATTERN = Pattern.compile("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?");
    static final Pattern ATOMIC_INTEGER_PATTERN = Pattern.compile("(?:0|[1-9][0-9]*)");

    private MarketDataContractValidation() {
    }

    static <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw rejected(MarketDataRejectionReason.NULL_VALUE, fieldName + " must not be null");
        }
        return value;
    }

    static String requireText(String value, String fieldName, Pattern pattern) {
        requireValue(value, fieldName);
        if (value.isBlank()) {
            throw rejected(MarketDataRejectionReason.BLANK_IDENTIFIER, fieldName + " must not be blank");
        }
        if (!pattern.matcher(value).matches()) {
            throw rejected(MarketDataRejectionReason.INVALID_IDENTIFIER, fieldName + " has invalid format");
        }
        return value;
    }

    static MarketDataContractViolation rejected(MarketDataRejectionReason reason, String message) {
        return new MarketDataContractViolation(Objects.requireNonNull(reason, "reason must not be null"), message);
    }
}
